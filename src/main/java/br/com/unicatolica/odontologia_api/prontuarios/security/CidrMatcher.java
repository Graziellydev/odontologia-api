package br.com.unicatolica.odontologia_api.prontuarios.security;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

public final class CidrMatcher {
    private final byte[] network;
    private final int prefix;

    public CidrMatcher(String cidr) {
        String[] parts = cidr.trim().split("/", -1);
        if (parts.length != 2 || !isNumericAddress(parts[0])) {
            throw new IllegalArgumentException("CIDR inválido: " + cidr);
        }
        try {
            network = InetAddress.getByName(parts[0]).getAddress();
            prefix = Integer.parseInt(parts[1]);
        } catch (UnknownHostException | NumberFormatException e) {
            throw new IllegalArgumentException("CIDR inválido: " + cidr, e);
        }
        if (prefix < 0 || prefix > network.length * 8) {
            throw new IllegalArgumentException("Prefixo CIDR inválido: " + cidr);
        }
    }

    public boolean matches(String address) {
        if (!isNumericAddress(address)) return false;
        try {
            byte[] candidate = InetAddress.getByName(address).getAddress();
            if (candidate.length != network.length) return false;
            int fullBytes = prefix / 8;
            int remainingBits = prefix % 8;
            if (!Arrays.equals(Arrays.copyOf(network, fullBytes), Arrays.copyOf(candidate, fullBytes))) return false;
            if (remainingBits == 0) return true;
            int mask = 0xff << (8 - remainingBits);
            return (network[fullBytes] & mask) == (candidate[fullBytes] & mask);
        } catch (UnknownHostException e) {
            return false;
        }
    }

    public static boolean isNumericAddress(String value) {
        if (value == null || value.isBlank() || value.indexOf('%') >= 0) return false;
        return value.matches("[0-9.]+") || value.matches("[0-9a-fA-F:.]+");
    }
}
