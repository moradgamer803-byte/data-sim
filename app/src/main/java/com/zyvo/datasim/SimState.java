package com.zyvo.datasim;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Snapshot of the device's SIM + mobile-data state, read through root. */
final class SimState {

    static final class Sim {
        int subId;
        int slot;
        String name;
    }

    boolean sawIsub = false;
    int dataSubId = -1;
    boolean mobileDataOn = false;
    String raw = "";
    final List<Sim> sims = new ArrayList<>();

    boolean rootOk() {
        return sawIsub;
    }

    Sim simBySub(int subId) {
        for (Sim s : sims) {
            if (s.subId == subId) {
                return s;
            }
        }
        return null;
    }

    String dataSimName() {
        Sim s = simBySub(dataSubId);
        return s == null ? ("SIM " + dataSubId) : s.name;
    }

    static SimState read() {
        SimState st = new SimState();
        RootShell.Result r = RootShell.run(
                "dumpsys isub; echo '---MD---'; settings get global mobile_data", 12000);
        st.raw = r.out;
        String out = r.out == null ? "" : r.out;

        int mdIdx = out.indexOf("---MD---");
        String mdPart = "";
        String isub = out;
        if (mdIdx >= 0) {
            mdPart = out.substring(mdIdx + 8).trim();
            isub = out.substring(0, mdIdx);
        }
        st.mobileDataOn = mdPart.startsWith("1");
        st.sawIsub = isub.contains("SubscriptionController");

        Matcher dd = Pattern.compile("defaultDataSubId=(-?\\d+)").matcher(isub);
        if (dd.find()) {
            st.dataSubId = Integer.parseInt(dd.group(1));
        }

        int a = isub.indexOf("ActiveSubInfoList:");
        if (a >= 0) {
            int b = isub.indexOf("AllSubInfoList:", a);
            if (b < 0) {
                b = isub.length();
            }
            String block = isub.substring(a, b);
            Matcher m = Pattern.compile(
                    "\\{id=(\\d+)[^}]*?simSlotIndex=(-?\\d+)[^}]*?displayName=([^\\s]+)").matcher(block);
            while (m.find()) {
                try {
                    Sim s = new Sim();
                    s.subId = Integer.parseInt(m.group(1));
                    s.slot = Integer.parseInt(m.group(2));
                    s.name = m.group(3);
                    if (s.name == null || s.name.isEmpty()) {
                        s.name = "SIM " + s.subId;
                    }
                    if (s.slot >= 0) {
                        st.sims.add(s);
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return st;
    }

    /** Verified working switch: set default data sub, bounce data, read back. */
    static RootShell.Result switchTo(int subId) {
        String cmd = "settings put global multi_sim_data_call " + subId
                + "; svc data disable; sleep 2; svc data enable; sleep 3; "
                + "dumpsys isub | grep -m1 defaultDataSubId";
        return RootShell.run(cmd, 25000);
    }

    static RootShell.Result setData(boolean on) {
        return RootShell.run("svc data " + (on ? "enable" : "disable"), 12000);
    }
}
