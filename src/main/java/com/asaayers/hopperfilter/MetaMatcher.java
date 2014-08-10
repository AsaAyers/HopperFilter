package com.asaayers.hopperfilter;

import java.util.Set;

/**
 * Created by asa on 8/9/14.
 */
public class MetaMatcher {

    public Set<Matcher> getMatchers() {
        return matchers;
    }

    private final Set<Matcher> matchers;

    MetaMatcher(Set<Matcher> matchers) {
        this.matchers = matchers;
    }
}
