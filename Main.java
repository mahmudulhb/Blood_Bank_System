import java.io.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

abstract class User {
    protected String name, bloodGroup, phone, address;
    protected int age;

    User(String n, int a, String bg, String ph, String addr) {
        name = n;
        age = a;
        bloodGroup = bg;
        phone = ph;
        address = addr;
    }
}

class Donor extends User {
    String lastDate;
    int count;

    Donor(String n, int a, String bg, String ph, String addr, String ld, int c) {
        super(n, a, bg, ph, addr);
        lastDate = ld;
        count = c;
    }

    boolean isEligible() {
        if (age < 18)
            return false;
        if (lastDate.equalsIgnoreCase("None"))
            return true;
        try {
            return ChronoUnit.MONTHS.between(LocalDate.parse(lastDate), LocalDate.now()) >= 3;
        } catch (Exception e) {
            return true;
        }
    }

    String status() {
        return isEligible() ? "[ELIGIBLE]" : "[WAITING] ";
    }

    String toTxt() {
        return String.join(",", name, age + "", bloodGroup, phone, address, lastDate, count + "");
    }

    public String toString() {
        return String.format("%-14s BG:%-4s Ph:%-13s Donations:%-3d Last:%-12s %s",
                name, bloodGroup, phone, count, lastDate, status());
    }
}

class Request {
    String patient, bloodGroup, location, contact;
    int needed, collected;

    Request(String p, String bg, int n, String loc, String con, int col) {
        patient = p;
        bloodGroup = bg;
        needed = n;
        location = loc;
        contact = con;
        collected = col;
    }

    boolean fulfilled() {
        return collected >= needed;
    }

    String toTxt() {
        return String.join(",", patient, bloodGroup, needed + "", location, contact, collected + "");
    }

    public String toString() {
        return String.format("[%s] %-12s %d/%d bags  Loc:%s  Contact:%s  %s",
                bloodGroup, patient, collected, needed, location, contact,
                fulfilled() ? "[DONE]" : "[OPEN]");
    }
}

class Store {
    static final String DF = "donors.txt", RF = "requests.txt";

    static List<String> read(String f) {
        List<String> l = new ArrayList<>();
        File file = new File(f);
        if (!file.exists())
            return l;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String s;
            while ((s = br.readLine()) != null)
                if (!s.trim().isEmpty())
                    l.add(s);
        } catch (IOException ignored) {
        }
        return l;
    }

    static void write(String f, List<String> l) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f))) {
            for (String s : l) {
                bw.write(s);
                bw.newLine();
            }
        } catch (IOException ignored) {
        }
    }

    static List<Donor> donors() {
        List<Donor> list = new ArrayList<>();
        for (String s : read(DF)) {
            String[] p = s.split(",");
            if (p.length >= 7)
                list.add(new Donor(p[0], Integer.parseInt(p[1]), p[2], p[3], p[4], p[5], Integer.parseInt(p[6])));
        }
        return list;
    }

    static List<Request> requests() {
        List<Request> list = new ArrayList<>();
        for (String s : read(RF)) {
            String[] p = s.split(",");
            if (p.length >= 6)
                list.add(new Request(p[0], p[1], Integer.parseInt(p[2]), p[3], p[4], Integer.parseInt(p[5])));
        }
        return list;
    }

    static void saveDonor(Donor d) {
        List<String> lines = new ArrayList<>();
        boolean found = false;
        for (Donor x : donors()) {
            if (x.phone.equals(d.phone)) {
                lines.add(d.toTxt());
                found = true;
            } else
                lines.add(x.toTxt());
        }
        if (!found)
            lines.add(d.toTxt());
        write(DF, lines);
    }

    static void saveRequest(Request r) {
        List<String> l = read(RF);
        l.add(r.toTxt());
        write(RF, l);
    }

    static void updateRequest(int i, Request r) {
        List<String> l = read(RF);
        if (i >= 0 && i < l.size()) {
            l.set(i, r.toTxt());
            write(RF, l);
        }
    }

    static boolean deleteDonor(String phone) {
        List<Donor> all = donors();
        boolean removed = all.removeIf(d -> d.phone.equals(phone));
        if (removed)
            write(DF, all.stream().map(Donor::toTxt).collect(Collectors.toList()));
        return removed;
    }
}

public class Main {
    static Scanner sc = new Scanner(System.in);

    static String input(String label) {
        System.out.print(label + ": ");
        return sc.nextLine().trim();
    }

    static int inputInt(String l) {
        try {
            return Integer.parseInt(input(l));
        } catch (Exception e) {
            return 0;
        }
    }

    static void ok(String m) {
        System.out.println("[OK] " + m);
    }

    static void err(String m) {
        System.out.println("[!]  " + m);
    }

    static void line() {
        System.out.println("-".repeat(72));
    }

    public static void main(String[] args) {
        while (true) {
            System.out.println("\n======= BLOOD BANK SYSTEM =======");
            System.out.println("1. Post Blood Request\n2. Donor Portal\n3. Admin Panel\n4. Exit");
            switch (input("Choice")) {
                case "1" -> postRequest();
                case "2" -> donorPortal();
                case "3" -> adminPortal();
                case "4" -> {
                    ok("Goodbye!");
                    return;
                }
                default -> err("Invalid choice.");
            }
        }
    }

    static void postRequest() {
        Store.saveRequest(new Request(
                input("Patient"), input("Blood Group (eg A+)").toUpperCase(),
                inputInt("Units Needed"), input("Hospital"), input("Contact"), 0));
        ok("Request posted!");
    }

    static void donorPortal() {
        while (true) {
            System.out.println("\n-- DONOR PORTAL --\n1.Register\n2.Login\n3.Back");
            switch (input("Choice")) {
                case "1" -> {
                    String ph = input("Phone");
                    if (Store.donors().stream().anyMatch(d -> d.phone.equals(ph))) {
                        err("Phone already registered.");
                        break;
                    }
                    Store.saveDonor(new Donor(
                            input("Name"), inputInt("Age"), input("Blood Group").toUpperCase(),
                            ph, input("Address"), input("Last Donation (YYYY-MM-DD or None)"), 0));
                    ok("Registered!");
                }
                case "2" -> {
                    String ph = input("Phone");
                    Donor d = Store.donors().stream().filter(x -> x.phone.equals(ph)).findFirst().orElse(null);
                    if (d == null)
                        err("Not found.");
                    else
                        donorDashboard(d);
                }
                case "3" -> {
                    return;
                }
            }
        }
    }

    static void donorDashboard(Donor d) {
        while (true) {
            System.out.printf("\n-- WELCOME %s | BG:%s | Donations:%d | Last:%s | %s --%n",
                    d.name.toUpperCase(), d.bloodGroup, d.count, d.lastDate, d.status());
            System.out.println("1.Edit Profile\n2.Log External Donation\n3.Volunteer for Request\n4.Logout");
            switch (input("Choice")) {
                case "1" -> {
                    String n = input("New Name (blank=keep)");
                    if (!n.isEmpty())
                        d.name = n;
                    String a = input("New Address (blank=keep)");
                    if (!a.isEmpty())
                        d.address = a;
                    Store.saveDonor(d);
                    ok("Profile updated.");
                }
                case "2" -> {
                    d.lastDate = input("Date (YYYY-MM-DD)");
                    d.count++;
                    Store.saveDonor(d);
                    ok("Donation logged. Total: " + d.count);
                }
                case "3" -> {
                    if (!d.isEligible()) {
                        err("Not eligible yet.");
                        break;
                    }
                    assignFlow(d);
                }
                case "4" -> {
                    return;
                }
            }
        }
    }

    static void adminPortal() {
        if (!input("Password").equals("admin123")) {
            err("Wrong password.");
            return;
        }
        while (true) {
            System.out.println(
                    "\n-- ADMIN PANEL --\n1.All Donors\n2.Eligible Donors\n3.Assign Donor\n4.Delete Donor\n5.Back");
            switch (input("Choice")) {
                case "1" -> {
                    line();
                    Store.donors().forEach(System.out::println);
                    line();
                }
                case "2" -> {
                    line();
                    Store.donors().stream().filter(Donor::isEligible).forEach(System.out::println);
                    line();
                }
                case "3" -> assignFlow(null);
                case "4" -> {
                    String ph = input("Donor Phone to delete");
                    if (Store.deleteDonor(ph))
                        ok("Deleted.");
                    else
                        err("Not found.");
                }
                case "5" -> {
                    return;
                }
            }
        }
    }

    static void assignFlow(Donor current) {
        List<Request> allRequests = Store.requests();

        List<int[]> openIndices = new ArrayList<>();
        List<Request> openRequests = new ArrayList<>();
        for (int i = 0; i < allRequests.size(); i++) {
            if (!allRequests.get(i).fulfilled()) {
                openIndices.add(new int[] { openRequests.size(), i });
                openRequests.add(allRequests.get(i));
            }
        }

        if (openRequests.isEmpty()) {
            err("No open requests available.");
            return;
        }

        line();
        for (int[] pair : openIndices)
            System.out.println("ID " + pair[0] + ": " + allRequests.get(pair[1]));
        line();

        int displayId = inputInt("Select Request ID");
        if (displayId < 0 || displayId >= openRequests.size()) {
            err("Invalid ID.");
            return;
        }

        Donor target = current;
        if (target == null) {
            line();
            Store.donors().stream().filter(Donor::isEligible).forEach(System.out::println);
            line();
            String ph = input("Donor Phone");
            target = Store.donors().stream().filter(x -> x.phone.equals(ph)).findFirst().orElse(null);
        }

        if (target == null || !target.isEligible()) {
            err("Donor ineligible or not found.");
            return;
        }

        int realIndex = openIndices.get(displayId)[1];
        Request r = allRequests.get(realIndex);

        if (r.collected >= r.needed) {
            err("Request already has enough donations.");
            return;
        }

        r.collected++;
        target.lastDate = LocalDate.now().toString();
        target.count++;

        Store.updateRequest(realIndex, r);
        Store.saveDonor(target);

        ok("Assigned! " + target.name + " -> " + r.patient
                + " (" + r.collected + "/" + r.needed + " bags)");
        if (r.fulfilled())
            ok("Request for " + r.patient + " is now fully fulfilled!");
    }
}
