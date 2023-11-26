package com.morlinnn.hostchecker;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class AddressFilter {
    private final Set<String> addressFilter;
    private final Set<String> regexpFilter;
    public AddressFilter(Set<String> addressFilter, Set<String> regexpFilter) {
        this.addressFilter = addressFilter;
        this.regexpFilter = regexpFilter;
    }

    public static final Set<String> LOOPBACK_ADDRESS = new HashSet<>();
    public static final Set<String> LOOPBACK_REGEXP = new HashSet<>();
    static {
        LOOPBACK_REGEXP.add("127.\\d+.\\d+.\\d+");
        LOOPBACK_REGEXP.add("^[0:][0:]+1$");
        LOOPBACK_ADDRESS.add("::1");
    }

    public static AddressFilter getLoopbackFilter() {
        return new AddressFilter(LOOPBACK_ADDRESS, LOOPBACK_REGEXP);
    }

    /**
     * 通过过滤列表检查address是否需要被过滤
     * @param address
     * @return true则是需要被过滤, false是不被过滤
     */
    public boolean filterAddress(String address) {
        if (regexpFilter != null) {
            for (String regex : regexpFilter) {
                if (Pattern.matches(regex, address)) return true;
            }
        }

        if (address == null) return false;

        return addressFilter.contains(address);
    }
}
