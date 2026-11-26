package br.com.unicatolica.odontologia_api.relatorios.service;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import br.com.unicatolica.odontologia_api.relatorios.dto.DistribuicaoItemResponse;
import br.com.unicatolica.odontologia_api.relatorios.dto.FiltrosRelatorioResponse;
import br.com.unicatolica.odontologia_api.relatorios.dto.RelatorioDemandaClinicaResponse;
import br.com.unicatolica.odontologia_api.relatorios.dto.RelatorioDemograficoResponse;
import br.com.unicatolica.odontologia_api.relatorios.repository.RelatorioCoordenacaoRepository;
import br.com.unicatolica.odontologia_api.relatorios.repository.RelatorioCoordenacaoRepository.Contagem;
import br.com.unicatolica.odontologia_api.relatorios.repository.RelatorioCoordenacaoRepository.EventoPaciente;

@Service
public class RelatorioCoordenacaoService {
    private static final List<String> FAIXAS = List.of("Até 17 anos", "18 a 29 anos", "30 a 44 anos", "45 a 59 anos", "60 anos ou mais");
    private static final String OUTROS = "Outros bairros / municípios";
    private final RelatorioCoordenacaoRepository repository;

    public RelatorioCoordenacaoService(RelatorioCoordenacaoRepository repository) { this.repository = repository; }

    public RelatorioDemograficoResponse demografico(String semestre, String dataInicio, String dataFim, String especialidade, String regiao) {
        FiltrosRelatorioResponse filtros = filtros(semestre, dataInicio, dataFim, especialidade, regiao);
        List<EventoPaciente> eventos = repository.buscarEventos(semestre, filtros.dataInicio(), filtros.dataFim(), especialidade, filtros.regiao());
        Map<Long, EventoPaciente> unicos = new LinkedHashMap<>();
        for (EventoPaciente e : eventos) unicos.putIfAbsent(e.id(), e);
        long total = unicos.size();
        TreeMap<String, Long> idades = new TreeMap<>(Comparator.comparingInt(FAIXAS::indexOf));
        FAIXAS.forEach(f -> idades.put(f, 0L));
        Map<String, Long> bairros = new LinkedHashMap<>();
        for (EventoPaciente e : unicos.values()) {
            int idade = Period.between(e.nascimento(), e.dataEvento()).getYears();
            String faixa = idade <= 17 ? FAIXAS.get(0) : idade <= 29 ? FAIXAS.get(1) : idade <= 44 ? FAIXAS.get(2) : idade <= 59 ? FAIXAS.get(3) : FAIXAS.get(4);
            idades.merge(faixa, 1L, Long::sum);
            String bairro = e.bairro() == null || e.bairro().isBlank() ? "Origem não informada" : e.bairro().trim();
            bairros.merge(bairro, 1L, Long::sum);
        }
        List<DistribuicaoItemResponse> faixas = idades.entrySet().stream().map(e -> item(e.getKey(), e.getValue(), total)).toList();
        List<DistribuicaoItemResponse> territorios = territorio(bairros, total);
        DistribuicaoItemResponse faixaTop = idades.entrySet().stream().max(Map.Entry.comparingByValue()).filter(e -> e.getValue() >= 3).map(e -> item(e.getKey(), e.getValue(), total)).orElse(null);
        DistribuicaoItemResponse bairroTop = territorios.stream().filter(i -> !OUTROS.equals(i.categoria())).findFirst().orElse(null);
        return new RelatorioDemograficoResponse(filtros,
                new RelatorioDemograficoResponse.BaseAnalise(total, "Pacientes únicos no recorte"), faixaTop, bairroTop, faixas, territorios);
    }

    public RelatorioDemandaClinicaResponse demanda(String semestre, String dataInicio, String dataFim, String especialidade, String regiao) {
        FiltrosRelatorioResponse filtros = filtros(semestre, dataInicio, dataFim, especialidade, regiao);
        List<Contagem> triagens = repository.buscarDemanda(true, semestre, filtros.dataInicio(), filtros.dataFim(), especialidade, filtros.regiao());
        List<Contagem> consultas = repository.buscarDemanda(false, semestre, filtros.dataInicio(), filtros.dataFim(), especialidade, filtros.regiao());
        return new RelatorioDemandaClinicaResponse(filtros, triagens.stream().mapToLong(Contagem::quantidade).sum(),
                consultas.stream().mapToLong(Contagem::quantidade).sum(), distribuição(triagens), distribuição(consultas),
                "Triagens representam demanda identificada; consultas representam agendamentos por procedimento.");
    }

    public String exportarDemografico(RelatorioDemograficoResponse r, String formato) { return exportar("Perfil demográfico", r, formato); }
    public String exportarDemanda(RelatorioDemandaClinicaResponse r, String formato) { return exportar("Demanda clínica", r, formato); }

    private String exportar(String titulo, Object response, String formato) {
        String aviso = "Relatório agregado; não contém identificadores pessoais.";
        if ("JSON".equalsIgnoreCase(formato)) {
            try { return new tools.jackson.databind.ObjectMapper().writeValueAsString(Map.of("relatorio", titulo, "aviso", aviso, "dados", response)); }
            catch (tools.jackson.core.JacksonException ex) { throw new IllegalStateException("Falha serializando relatório agregado.", ex); }
        }
        if (!"CSV".equalsIgnoreCase(formato)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato deve ser CSV ou JSON.");
        StringBuilder csv = new StringBuilder("relatorio,categoria,quantidade,percentual\n");
        csv.append(escape(titulo)).append(',').append(escape(aviso)).append(",,\n");
        List<DistribuicaoItemResponse> itens = response instanceof RelatorioDemograficoResponse d ? concat(d.distribuicaoFaixaEtaria(), d.distribuicaoTerritorial()) : concat(((RelatorioDemandaClinicaResponse)response).porProcedimento(), ((RelatorioDemandaClinicaResponse)response).porEspecialidade());
        for (DistribuicaoItemResponse i : itens) csv.append(escape(titulo)).append(',').append(escape(i.categoria())).append(',').append(i.quantidade()).append(',').append(i.percentual()).append('\n');
        return csv.toString();
    }

    private List<DistribuicaoItemResponse> concat(List<DistribuicaoItemResponse> a, List<DistribuicaoItemResponse> b) { List<DistribuicaoItemResponse> out = new ArrayList<>(a); out.addAll(b); return out; }
    private String escape(String s) { return "\"" + s.replace("\"", "\"\"") + "\""; }
    private List<DistribuicaoItemResponse> distribuição(List<Contagem> counts) { long total = counts.stream().mapToLong(Contagem::quantidade).sum(); return counts.stream().map(c -> item(c.categoria(), c.quantidade(), total)).toList(); }
    private List<DistribuicaoItemResponse> territorio(Map<String, Long> bairros, long total) {
        List<Map.Entry<String, Long>> sorted = bairros.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry::getKey)).toList();
        List<DistribuicaoItemResponse> result = new ArrayList<>(); long outros = 0; int exibidos = 0;
        for (Map.Entry<String, Long> e : sorted) {
            if (e.getValue() < 3 || exibidos == 4) outros += e.getValue();
            else { result.add(item(e.getKey(), e.getValue(), total)); exibidos++; }
        }
        if (outros > 0) result.add(item(OUTROS, outros, total));
        return List.copyOf(result);
    }
    private DistribuicaoItemResponse item(String label, long count, long total) { return new DistribuicaoItemResponse(label, count, total == 0 ? 0 : Math.round(count * 1000.0 / total) / 10.0); }

    private FiltrosRelatorioResponse filtros(String semestre, String inicio, String fim, String especialidade, String regiao) {
        if (semestre != null && !semestre.matches("\\d{4}\\.[12]")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Semestre deve seguir AAAA.1 ou AAAA.2.");
        LocalDate d1 = parse(inicio), d2 = parse(fim);
        if (d1 != null && d2 != null && d1.isAfter(d2)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dataInicio não pode ser posterior a dataFim.");
        return new FiltrosRelatorioResponse(semestre, d1, d2, blank(especialidade), regiao == null || regiao.isBlank() ? "TODOS" : regiao.trim());
    }
    private LocalDate parse(String s) { if (s == null || s.isBlank()) return null; try { return LocalDate.parse(s); } catch (DateTimeParseException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data deve seguir YYYY-MM-DD."); } }
    private String blank(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
