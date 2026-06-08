import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

// ═══════════════════════════════════════════════════════════════════════════════
//  Package-private top-level structural classes (kept in BackendEngine.java)
// ═══════════════════════════════════════════════════════════════════════════════

abstract class User {
    protected String userId, username, passwordHash, email;
    protected BackendEngine.UserRole role;
    protected boolean loggedIn;
    protected long lastLogin;
    protected int loginCount;

    public User(String uid, String uname, String pwHash,
                BackendEngine.UserRole role, String email) {
        this.userId = uid; this.username = uname; this.passwordHash = pwHash;
        this.role = role;  this.email = email;
    }

    public boolean login(String u, String p) {
        if (this.username.equals(u) && BackendEngine.verifyPassword(p, this.passwordHash)) {
            loggedIn = true; lastLogin = System.currentTimeMillis(); loginCount++; return true;
        }
        return false;
    }
    public void    logout()                    { loggedIn = false; }
    public String  getUserId()                 { return userId; }
    public String  getUsername()               { return username; }
    public BackendEngine.UserRole getRole()    { return role; }
    public boolean isLoggedIn()                { return loggedIn; }
    public String  getEmail()                  { return email; }
    public long    getLastLogin()              { return lastLogin; }
    public int     getLoginCount()             { return loginCount; }
    public abstract String   getRoleDescription();
    public abstract String[] getPermissions();
}

// ── Admin ─────────────────────────────────────────────────────────────────────
class Admin extends User {
    private final String adminToken;
    private boolean maintenanceMode;
    public Admin(String uid, String uname, String pw, String email) {
        super(uid, uname, pw, BackendEngine.UserRole.ADMIN, email);
        this.adminToken = BackendEngine.sha256(uid+uname).substring(0,16).toUpperCase();
    }
    public void configureFirewallRules(Firewall fw, String r){ fw.addRule(r); }
    public List<String> clearLogs(SecurityLog log)           { return log.clearAndArchive(); }
    public String  getAdminToken()                           { return adminToken; }
    public boolean isMaintenanceMode()                       { return maintenanceMode; }
    public void    setMaintenanceMode(boolean m)             { maintenanceMode = m; }
    @Override public String getRoleDescription() {
        return "Full system administrator. Manages users, firewall, IDS, logs, and all platform settings.";
    }
    @Override public String[] getPermissions() {
        return new String[]{"SIMULATE_ATTACKS","VIEW_LOGS","CLEAR_LOGS","MANAGE_USERS",
            "CONFIGURE_FIREWALL","MANAGE_IDS","VIEW_REPORTS","SYSTEM_ADMIN","EXPORT_DATA",
            "DELETE_USERS","TOGGLE_FIREWALL","AUDIT_TRAIL","RESET_IDS"};
    }
}

// ── SecurityAnalyst ───────────────────────────────────────────────────────────
class SecurityAnalyst extends User {
    private String classificationLevel;
    private int    reportsGenerated;
    public SecurityAnalyst(String uid, String uname, String pw, String email, String lvl) {
        super(uid, uname, pw, BackendEngine.UserRole.ANALYST, email);
        this.classificationLevel = lvl;
    }
    public List<String> analyzeThreats(SecurityLog log){ return log.readLogs(); }
    public String generateSecurityReport(SecurityLog log, List<BackendEngine.AttackResult> history) {
        List<String> entries = log.readLogs(); reportsGenerated++;
        StringBuilder r = new StringBuilder();
        r.append("╔══════════════════════════════════════════════════════════════╗\n");
        r.append("║        CTTMS SECURITY COMPLIANCE REPORT v").append(BackendEngine.VERSION).append("            ║\n");
        r.append("╚══════════════════════════════════════════════════════════════╝\n\n");
        r.append("  Analyst     : ").append(username.toUpperCase()).append("\n");
        r.append("  Clearance   : ").append(classificationLevel).append("\n");
        r.append("  Generated   : ").append(BackendEngine.timestamp()).append("\n");
        r.append("  Report #    : ").append(reportsGenerated).append("\n\n");
        r.append("── LOG SUMMARY ────────────────────────────────────────────────\n");
        long crit=entries.stream().filter(e->e.contains("[CRITICAL]")).count();
        long warn=entries.stream().filter(e->e.contains("[WARNING]")).count();
        long info=entries.stream().filter(e->e.contains("[INFO]")).count();
        r.append(String.format("  CRITICAL : %d\n  WARNING  : %d\n  INFO     : %d\n  TOTAL    : %d\n\n",
            crit,warn,info,entries.size()));
        r.append("── SIMULATION HISTORY ─────────────────────────────────────────\n");
        if (history.isEmpty()) { r.append("  No simulations recorded.\n\n"); }
        else {
            long breaches=history.stream().filter(h->h.breached).count();
            long blocked=history.size()-breaches;
            r.append(String.format("  Total : %d | Breaches : %d | Neutralized : %d | Rate : %.1f%%\n\n",
                history.size(),breaches,blocked,history.size()>0?(breaches*100.0/history.size()):0));
            Map<String,long[]> ts=new LinkedHashMap<>();
            for (BackendEngine.AttackResult ar:history) {
                ts.computeIfAbsent(ar.attackType,k->new long[2]);
                ts.get(ar.attackType)[0]++;
                if(ar.breached) ts.get(ar.attackType)[1]++;
            }
            for (Map.Entry<String,long[]> e:ts.entrySet())
                r.append(String.format("    %-42s %d runs, %d breaches\n",e.getKey(),e.getValue()[0],e.getValue()[1]));
            r.append("\n");
        }
        r.append("── RECENT LOG EVENTS (last 30) ────────────────────────────────\n");
        int show=Math.min(entries.size(),30);
        for (int i=entries.size()-show;i<entries.size();i++) r.append("  ").append(entries.get(i)).append("\n");
        r.append("\n── RECOMMENDATIONS ────────────────────────────────────────────\n");
        if (crit>0) r.append("  [!] CRITICAL events detected. Immediate review required.\n");
        if (history.stream().anyMatch(h->h.breached)) r.append("  [!] Breaches recorded. Patch weak points immediately.\n");
        r.append("  [*] Phishing awareness training — quarterly.\n");
        r.append("  [*] MFA on all privileged accounts.\n");
        r.append("  [*] Review firewall rules for coverage gaps.\n");
        r.append("\n╔══════════════════════════════════════════════════════════════╗\n");
        r.append("║                      END OF REPORT                          ║\n");
        r.append("╚══════════════════════════════════════════════════════════════╝\n");
        return r.toString();
    }
    public String getClassificationLevel() { return classificationLevel; }
    public int    getReportsGenerated()     { return reportsGenerated; }
    @Override public String getRoleDescription() {
        return "Threat analyst: monitoring, log inspection, simulation access, compliance reporting.";
    }
    @Override public String[] getPermissions() {
        return new String[]{"SIMULATE_ATTACKS","VIEW_LOGS","VIEW_REPORTS","ANALYZE_THREATS",
            "EXPORT_DATA","VIEW_FIREWALL","VIEW_IDS_ALERTS"};
    }
}

// ── Trainee ───────────────────────────────────────────────────────────────────
class Trainee extends User {
    private final AtomicInteger simulationsRun = new AtomicInteger(0);
    private final AtomicInteger quizScore      = new AtomicInteger(0);
    public Trainee(String uid, String uname, String pw, String email) {
        super(uid, uname, pw, BackendEngine.UserRole.TRAINEE, email);
    }
    public void recordSimulation()   { simulationsRun.incrementAndGet(); }
    public void setQuizScore(int s)  { quizScore.set(s); }
    public int  getSimulationsRun()  { return simulationsRun.get(); }
    public int  getQuizScore()       { return quizScore.get(); }
    @Override public String getRoleDescription() {
        return "Trainee: sandbox simulations and learning modules only.";
    }
    @Override public String[] getPermissions() {
        return new String[]{"SIMULATE_ATTACKS","VIEW_OWN_LOGS","LEARNING_MODULE","VIEW_BASIC_STATS"};
    }
}

// ── Firewall ──────────────────────────────────────────────────────────────────
class Firewall {
    private final List<String>         rules;
    private final Map<String,Integer>  hitCounts;
    private int     thresholdSeverity;
    private boolean enabled;
    private int     packetsInspected, packetsBlocked;

    public Firewall(int threshold) {
        rules = new CopyOnWriteArrayList<>();
        hitCounts = new ConcurrentHashMap<>();
        thresholdSeverity = threshold; enabled = true;
        initDefaultRules();
    }
    private void initDefaultRules() {
        rules.add("BLOCK:DDOS_FLOOD:severity>7");
        rules.add("BLOCK:MITM_ARP_SPOOF:severity>6");
        rules.add("BLOCK:SQLI_UNION_TAUTOLOGY:severity>5");
        rules.add("BLOCK:RANSOMWARE_EXECUTION:severity>8");
        rules.add("BLOCK:PHISHING_DOMAIN_SPOOF:severity>4");
        rules.add("BLOCK:BRUTE_FORCE_AUTH:severity>3");
    }
    public boolean filterTraffic(String sig, int severity) {
        packetsInspected++;
        hitCounts.merge(sig,1,Integer::sum);
        if (!enabled) return false;
        for (String rule:rules) {
            String[] p=rule.split(":");
            if (p.length>=3&&p[1].equals(sig)) {
                int thr=Integer.parseInt(p[2].replace("severity>",""));
                if (severity>thr){ packetsBlocked++; return false; }
            }
        }
        return severity<=thresholdSeverity;
    }
    public void addRule(String r)              { rules.add(r); }
    public void removeRule(int i)              { if(i>=0&&i<rules.size()) rules.remove(i); }
    public List<String> getRules()             { return Collections.unmodifiableList(rules); }
    public Map<String,Integer> getHitCounts()  { return Collections.unmodifiableMap(hitCounts); }
    public int  getThresholdSeverity()         { return thresholdSeverity; }
    public void setThresholdSeverity(int t)    { thresholdSeverity=t; }
    public boolean isEnabled()                 { return enabled; }
    public void setEnabled(boolean e)          { enabled=e; }
    public int  getPacketsInspected()          { return packetsInspected; }
    public int  getPacketsBlocked()            { return packetsBlocked; }
}

// ── ThreatDetector ────────────────────────────────────────────────────────────
class ThreatDetector {
    private final AtomicInteger  alertCount;
    private final List<String>   alertHistory;
    private Consumer<String>     alertCallback;
    private boolean              active;
    private String               detectionMode;
    public ThreatDetector() {
        alertCount   = new AtomicInteger(0);
        alertHistory = Collections.synchronizedList(new ArrayList<>());
        active=true; detectionMode="HEURISTIC";
    }
    public void setAlertCallback(Consumer<String> cb) { alertCallback=cb; }
    public void assessEvent(String desc, int sev) {
        if (!active) return;
        alertCount.incrementAndGet();
        String alert="["+BackendEngine.timestamp()+"][THREAT-DETECTED] Sev="+sev+" | "+desc;
        alertHistory.add(alert);
        if (alertCallback!=null) alertCallback.accept(alert);
    }
    public int         getAlertCount()   { return alertCount.get(); }
    public List<String> getAlertHistory(){ return Collections.unmodifiableList(alertHistory); }
    public void reset()                  { alertCount.set(0); alertHistory.clear(); }
    public boolean isActive()            { return active; }
    public void setActive(boolean a)     { active=a; }
    public String getDetectionMode()     { return detectionMode; }
    public void setDetectionMode(String m){ detectionMode=m; }
}

// ── SecurityLog ───────────────────────────────────────────────────────────────
class SecurityLog {
    private final String       logPath;
    private final List<String> inMemoryLog;
    private final Object       lock = new Object();
    public SecurityLog(String path) {
        logPath=path; inMemoryLog=Collections.synchronizedList(new ArrayList<>());
    }
    public void writeLog(String level, String message) {
        String entry="["+BackendEngine.timestamp()+"]["+level+"] "+message;
        synchronized(lock) {
            inMemoryLog.add(entry);
            try {
                File f=new File(logPath); f.getParentFile().mkdirs();
                try(FileWriter fw=new FileWriter(f,true)){ fw.write(entry+System.lineSeparator()); }
            } catch(IOException ignored){}
        }
    }
    public List<String> readLogs() { synchronized(lock){ return new ArrayList<>(inMemoryLog); } }
    public List<String> clearAndArchive() {
        synchronized(lock){
            List<String> arc=new ArrayList<>(inMemoryLog); inMemoryLog.clear();
            try(FileWriter fw=new FileWriter(new File(logPath),false)){ /* truncate */ }
            catch(IOException ignored){}
            return arc;
        }
    }
    public int    getLogCount() { return inMemoryLog.size(); }
    public String getLogPath()  { return logPath; }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  BackendEngine — main public class
// ═══════════════════════════════════════════════════════════════════════════════
public class BackendEngine {

    public static final String VERSION = "3.1.0";

    public enum AttackType {
        DDOS        ("DDoS — Distributed Denial of Service",
            "Floods the target with massive traffic from botnet nodes, exhausting bandwidth.",9),
        MITM        ("MITM — Man-in-the-Middle",
            "Intercepts traffic by poisoning ARP tables and forging TLS certificates.",7),
        SQL_INJECTION("SQLi — SQL Injection",
            "Injects malicious SQL into unsanitized inputs to extract or destroy DB records.",8),
        RANSOMWARE  ("Ransomware Execution",
            "Encrypts victim filesystem with AES-256 and demands BTC for the decryption key.",10),
        PHISHING    ("Phishing / Spear-Phishing",
            "Spoofed emails with fraudulent links harvest credentials from unsuspecting users.",6),
        BRUTE_FORCE ("Brute Force / Credential Stuffing",
            "Automates dictionary-based and stuffed password attempts at high velocity.",7),
        ZERO_DAY    ("Zero-Day Exploit",
            "Exploits an undisclosed vulnerability before any patch is available.",10);

        public final String displayName, description;
        public final int    defaultSeverity;
        AttackType(String d, String desc, int s){ displayName=d; description=desc; defaultSeverity=s; }
    }

    public enum UserRole { ADMIN, ANALYST, TRAINEE }

    // ── Crypto helpers ─────────────────────────────────────────────────────────
    public static String sha256(String input) {
        try {
            MessageDigest md=MessageDigest.getInstance("SHA-256");
            byte[] h=md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb=new StringBuilder();
            for (byte b:h) sb.append(String.format("%02x",b));
            return sb.toString();
        } catch(NoSuchAlgorithmException e){ return input; }
    }
    public static String hashPassword(String password) {
        try {
            SecureRandom sr=new SecureRandom(); byte[] salt=new byte[16]; sr.nextBytes(salt);
            PBEKeySpec spec=new PBEKeySpec(password.toCharArray(),salt,310_000,256);
            SecretKeyFactory skf=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash=skf.generateSecret(spec).getEncoded(); spec.clearPassword();
            return toHex(salt)+":"+toHex(hash);
        } catch(Exception e){ return sha256(password); }
    }
    public static boolean verifyPassword(String password, String stored) {
        try {
            if (!stored.contains(":")) return sha256(password).equals(stored);
            String[] p=stored.split(":",2);
            byte[] salt=fromHex(p[0]), expected=fromHex(p[1]);
            PBEKeySpec spec=new PBEKeySpec(password.toCharArray(),salt,310_000,256);
            SecretKeyFactory skf=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] actual=skf.generateSecret(spec).getEncoded(); spec.clearPassword();
            return MessageDigest.isEqual(actual,expected);
        } catch(Exception e){ return false; }
    }
    private static String toHex(byte[] b){
        StringBuilder sb=new StringBuilder();
        for (byte x:b) sb.append(String.format("%02x",x)); return sb.toString();
    }
    private static byte[] fromHex(String hex){
        byte[] out=new byte[hex.length()/2];
        for(int i=0;i<hex.length();i+=2) out[i/2]=(byte)Integer.parseInt(hex.substring(i,i+2),16);
        return out;
    }
    public static String timestamp(){ return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()); }
    public static String severityLabel(int s){
        if(s<=3) return "LOW"; if(s<=6) return "MEDIUM"; if(s<=8) return "HIGH"; return "CRITICAL";
    }
    public static int scaledHealth(int hi, int lo, int sev){
        return (int)(hi-(hi-lo)*(sev/10f));
    }

    // ── AttackResult ───────────────────────────────────────────────────────────
    public static class AttackResult {
        public final boolean      breached;
        public final int          finalHealth, severityUsed;
        public final List<String> eventLog, strongPoints, weakPoints;
        public final String       attackType, summary, timestamp, initiator;
        public AttackResult(boolean b,int fh,int sev,List<String> el,
                            List<String> sp,List<String> wp,
                            String at,String sum,String init) {
            breached=b; finalHealth=fh; severityUsed=sev;
            eventLog=el; strongPoints=sp; weakPoints=wp;
            attackType=at; summary=sum; timestamp=timestamp(); initiator=init;
        }
    }

    // ── Abstract Attack ────────────────────────────────────────────────────────
    public static abstract class Attack {
        protected String attackId; protected int severityLevel;
        protected long startTime;  protected AttackType type;
        public Attack(String id,int sev,AttackType t){
            attackId=id; severityLevel=sev; startTime=System.currentTimeMillis(); type=t;
        }
        public abstract AttackResult executeAttack(Firewall fw, ThreatDetector det, String init);
        public void logAttackDetails(SecurityLog log){
            log.writeLog("INFO","["+type.displayName+"] id="+attackId
                +" sev="+severityLevel+"["+severityLabel(severityLevel)+"]");
        }
        protected String sevTag(){
            switch(severityLabel(severityLevel)){
                case "CRITICAL": return "[!!CRITICAL!!]";
                case "HIGH":     return "[HIGH]";
                case "MEDIUM":   return "[MEDIUM]";
                default:         return "[LOW]";
            }
        }
    }

    // ── DDoS ─────────────────────────────────────────────────────────────────
    public static class DDoSAttack extends Attack {
        private final int pps; private final String targetIp;
        public DDoSAttack(String id,int sev,int pps,String ip){
            super(id,sev,AttackType.DDOS); this.pps=pps; this.targetIp=ip;
        }
        @Override public AttackResult executeAttack(Firewall fw,ThreatDetector det,String init){
            String lbl=severityLabel(severityLevel); List<String> ev=new ArrayList<>();
            ev.add("[INIT] "+sevTag()+" DDoS botnet activated — "+pps+" pps targeting "+targetIp);
            if(severityLevel>=4) ev.add("[WAVE-1] UDP amplification flood via DNS reflectors ("+pps/2+" pps)");
            if(severityLevel>=5) ev.add("[WAVE-2] SYN flood — half-open connections saturating TCP queue");
            if(severityLevel>=7) ev.add("[WAVE-3] HTTP/2 layer-7 reflection via CDN mirror abuse");
            if(severityLevel>=9) ev.add("[WAVE-4] ICMP ping-of-death — fragmenting kernel network stack");
            ev.add("[FW] Rate-limiter engaged; BGP blackhole announced for attacker ASN");
            boolean blocked=fw.filterTraffic("DDOS_FLOOD",severityLevel);
            if(!blocked){
                ev.add("[BREACH] "+sevTag()+" Scrubbing center exhausted — all traffic passed to host");
                if(severityLevel>=8) ev.add("[IMPACT] CPU 100% — service 503 UNAVAILABLE — SLA violated");
                if(severityLevel>=9) ev.add("[AMPLIFY] Reflected traffic x"+(severityLevel*100)+"Gbps peak");
                det.assessEvent("DDoS BREACH ["+lbl+"] — "+targetIp,severityLevel);
            } else {
                ev.add("[MITIGATED] Anycast scrubbing absorbed flood — clean traffic forwarded");
                ev.add("[RECOVERY] ISP null-routed attack sources; availability preserved");
            }
            List<String> strong=Arrays.asList("Anycast network routing","CDN traffic distribution",
                "BGP blackholing configured","ISP DDoS scrubbing agreement");
            List<String> weak=severityLevel>=7
                ? Arrays.asList("No on-prem volumetric DDoS appliance","Single ISP uplink (no multi-homing)",
                    "UDP amplification not filtered upstream","No automated secondary DC failover")
                : Arrays.asList("Single ISP uplink","UDP amplification not filtered upstream");
            int health=blocked?scaledHealth(85,25,severityLevel):scaledHealth(18,2,severityLevel);
            String sum=blocked
                ?"["+lbl+"] DDoS absorbed. BGP blackholing held. Recommend dedicated hardware + multi-homing."
                :"["+lbl+"] BREACH: Full DDoS disruption. Immediate ISP coordination required.";
            return new AttackResult(!blocked,health,severityLevel,ev,strong,weak,type.displayName,sum,init);
        }
    }

    // ── MITM ─────────────────────────────────────────────────────────────────
    public static class MITMAttack extends Attack {
        private final String net, victimIp;
        public MITMAttack(String id,int sev,String net,String ip){
            super(id,sev,AttackType.MITM); this.net=net; this.victimIp=ip;
        }
        @Override public AttackResult executeAttack(Firewall fw,ThreatDetector det,String init){
            String lbl=severityLabel(severityLevel); List<String> ev=new ArrayList<>();
            ev.add("[RECON] "+sevTag()+" Network scan — victim "+victimIp+" on "+net);
            ev.add("[ARP] Gratuitous ARP broadcast — attacker MAC → gateway IP");
            ev.add("[ARP] Victim ARP table poisoned — traffic rerouted via attacker");
            if(severityLevel>=5) ev.add("[TLS-STRIP] HTTPS→HTTP downgrade attempted on active sessions");
            if(severityLevel>=7) ev.add("[CERT-FORGE] Rogue TLS cert: CN=corp-internal-verify.net");
            if(severityLevel>=8) ev.add("[CAPTURE] Decrypting stream — extracting plaintext credentials");
            ev.add("[IDS] Anomalous ARP reply frequency on switch port 0/14");
            boolean blocked=fw.filterTraffic("MITM_ARP_SPOOF",severityLevel);
            if(!blocked){
                ev.add("[BREACH] "+sevTag()+" Credentials captured in plaintext");
                if(severityLevel>=7) ev.add("[SESSION] Token cloned — attacker authenticated as victim");
                if(severityLevel>=9) ev.add("[EXFIL] 14 creds harvested — banking session active");
                det.assessEvent("MITM BREACH ["+lbl+"] — "+net,severityLevel);
            } else {
                ev.add("[BLOCKED] Dynamic ARP inspection dropped poisoned replies");
                ev.add("[BLOCKED] HSTS rejected TLS downgrade — connection reset");
            }
            List<String> strong=Arrays.asList("TLS 1.3 on primary services","HSTS max-age enforced",
                "Cert pinning on mobile client","Network segmentation");
            List<String> weak=severityLevel>=7
                ? Arrays.asList("No DAI on managed switches","Legacy HTTP on port 8080",
                    "DNS-over-HTTPS not enforced","No 802.1X port auth","No mTLS inside mesh")
                : Arrays.asList("Legacy HTTP on port 8080","DNS-over-HTTPS not enforced");
            int health=blocked?scaledHealth(88,22,severityLevel):scaledHealth(20,5,severityLevel);
            String sum=blocked
                ?"["+lbl+"] MITM blocked by DAI + HSTS. Legacy HTTP channels remain risky."
                :"["+lbl+"] BREACH: Full session hijack. Enforce DAI, mTLS, rotate creds immediately.";
            return new AttackResult(!blocked,health,severityLevel,ev,strong,weak,type.displayName,sum,init);
        }
    }

    // ── SQLi ──────────────────────────────────────────────────────────────────
    public static class SQLInjectionAttack extends Attack {
        private final String table, payload;
        public SQLInjectionAttack(String id,int sev,String tbl,String pl){
            super(id,sev,AttackType.SQL_INJECTION); table=tbl; payload=pl;
        }
        @Override public AttackResult executeAttack(Firewall fw,ThreatDetector det,String init){
            String lbl=severityLabel(severityLevel); List<String> ev=new ArrayList<>();
            ev.add("[RECON] "+sevTag()+" Fingerprint: MySQL on Apache/2.4.51");
            ev.add("[PROBE] Error-based injection — DB version leaked");
            if(severityLevel>=4) ev.add("[PROBE] Tautology: ' OR '1'='1 — evaluated");
            ev.add("[INJECT] Payload: "+payload);
            if(severityLevel>=6) ev.add("[UNION] UNION SELECT from information_schema");
            if(severityLevel>=8) ev.add("[BLIND] Time-delay: SLEEP(5) — async confirmed");
            if(severityLevel>=8) ev.add("[ENUM] Tables found: users, sessions, admin_credentials");
            ev.add("[WAF] OWASP CRS scanning SQL keyword patterns");
            boolean blocked=fw.filterTraffic("SQLI_UNION_TAUTOLOGY",severityLevel);
            if(!blocked){
                ev.add("[BREACH] "+sevTag()+" Parameterized query absent on /api/login");
                ev.add("[DUMP] "+table+" dumped — "+(200+new Random().nextInt(9800))+" records exfiltrated");
                if(severityLevel>=8) ev.add("[ESC] Admin hash cracked — full DB access");
                if(severityLevel>=9) ev.add("[PWNED] Schema mapped; DROP TABLE executed");
                det.assessEvent("SQLi BREACH ["+lbl+"] — table: "+table,severityLevel);
            } else {
                ev.add("[BLOCKED] Prepared statements neutralized injection");
                ev.add("[WAF] Rule 942100 triggered — IP banned 24h");
            }
            List<String> strong=Arrays.asList("Parameterized queries on primary endpoints",
                "WAF with OWASP CRS v3.3","Minimal DB privilege (SELECT only)","Error messages sanitized");
            List<String> weak=severityLevel>=7
                ? Arrays.asList("Legacy admin panel uses raw queries","/api/search has no input validation",
                    "Stored procedures not enforced everywhere","ORM inconsistent in 3 legacy modules")
                : Arrays.asList("Legacy admin panel uses raw queries","ORM inconsistent in legacy modules");
            int health=blocked?scaledHealth(86,15,severityLevel):scaledHealth(15,2,severityLevel);
            String sum=blocked
                ?"["+lbl+"] SQLi neutralised. Legacy admin panel raw queries remain critical risk."
                :"["+lbl+"] BREACH: DB compromised. Patch raw queries, rotate creds, audit access logs.";
            return new AttackResult(!blocked,health,severityLevel,ev,strong,weak,type.displayName,sum,init);
        }
    }

    // ── Ransomware ────────────────────────────────────────────────────────────
    public static class RansomwareAttack extends Attack {
        private final String keyId; private final int files;
        public RansomwareAttack(String id,int sev,String key,int f){
            super(id,sev,AttackType.RANSOMWARE); keyId=key; files=f;
        }
        @Override public AttackResult executeAttack(Firewall fw,ThreatDetector det,String init){
            String lbl=severityLabel(severityLevel); double btc=0.5+(severityLevel-1)*0.5;
            List<String> ev=new ArrayList<>();
            ev.add("[DELIVERY] "+sevTag()+" Ransomware dropper via malicious macro attachment");
            ev.add("[EXEC] Macro ran — dropper pulls payload from C2: 185.220.x.x");
            if(severityLevel>=5) ev.add("[PERSIST] HKCU\\Run\\WindowsUpdate = payload.exe");
            ev.add("[RECON] Targeting: .docx .xlsx .pdf .db .bak .zip");
            if(severityLevel>=7) ev.add("[VSS] vssadmin delete shadows /all /quiet");
            ev.add("[ENC] AES-256-CBC loop — key: "+keyId.substring(0,8)+"...");
            ev.add("[PROG] "+(files/4)+" files encrypted → .locked");
            ev.add("[PROG] "+(files/2)+" files encrypted — backup paths enumerated");
            if(severityLevel>=8) ev.add("[LATERAL] SMB: spreading to \\\\fileserver\\shared");
            ev.add("[EDR] Heuristic: mass rename/extension change detected");
            boolean blocked=fw.filterTraffic("RANSOMWARE_EXECUTION",severityLevel);
            if(!blocked){
                ev.add("[BREACH] "+sevTag()+" "+files+" files encrypted; ransom note deployed");
                ev.add(String.format("[C2] Key escrowed — demand: %.1f BTC",btc));
                if(severityLevel>=8) ev.add("[SPREAD] 6 shares encrypted; DC targeted");
                det.assessEvent("Ransomware BREACH ["+lbl+"] — "+files+" files",severityLevel);
            } else {
                ev.add("[BLOCKED] EDR killed payload.exe at "+(files/4)+" files");
                ev.add("[RECOVERY] Shadow copies intact — point-in-time restore initiated");
                ev.add("[QUARANTINE] Endpoint isolated; forensic image started");
            }
            List<String> strong=Arrays.asList("EDR with behavioral heuristics","Immutable offline backups (RPO 4h)",
                "Email sandbox detonation","SMB signing enforced","App allowlisting");
            List<String> weak=severityLevel>=8
                ? Arrays.asList("Office macros enabled by default","PowerShell/WScript unrestricted",
                    "Share permissions too broad","Backup restore untested","No egress filtering")
                : Arrays.asList("Office macros enabled","No egress filtering","Backup restore untested");
            int health=blocked?scaledHealth(78,18,severityLevel):scaledHealth(14,1,severityLevel);
            String sum=blocked
                ?"["+lbl+"] Ransomware contained by EDR. Backups intact. Enforce macro + egress policy."
                :"["+lbl+"] CATASTROPHIC: Full encryption"+(severityLevel>=8?" + lateral spread":"")+". Isolate, DO NOT pay, restore from backup.";
            return new AttackResult(!blocked,health,severityLevel,ev,strong,weak,type.displayName,sum,init);
        }
    }

    // ── Phishing ──────────────────────────────────────────────────────────────
    public static class PhishingAttack extends Attack {
        private final String targetEmail, spoofedDomain;
        public PhishingAttack(String id,int sev,String email,String domain){
            super(id,sev,AttackType.PHISHING); targetEmail=email; spoofedDomain=domain;
        }
        @Override public AttackResult executeAttack(Firewall fw,ThreatDetector det,String init){
            String lbl=severityLabel(severityLevel); List<String> ev=new ArrayList<>();
            ev.add("[OSINT] "+sevTag()+" Target: "+targetEmail+" identified via LinkedIn");
            if(severityLevel>=5) ev.add("[CRAFT] Spear-phish with personalised Q4 financial lure");
            ev.add("[SPOOF] FROM: cfo@"+spoofedDomain+" (DKIM via lookalike domain)");
            ev.add("[SUBJECT] URGENT: Q4 Wire Transfer Approval");
            if(severityLevel>=6) ev.add("[LURE] Body uses real project names from public posts");
            ev.add("[PAYLOAD] Link: https://"+spoofedDomain+"/approve?ref=Q4FIN");
            ev.add("[DELIVER] Bypassed spam filter — domain <24h old, not yet blacklisted");
            if(severityLevel>=7) ev.add("[TRACK] Tracking pixel opened at 09:14 UTC");
            ev.add("[CLICK] User clicked at 09:16 UTC — navigating to fake portal");
            boolean blocked=fw.filterTraffic("PHISHING_DOMAIN_SPOOF",severityLevel);
            if(!blocked){
                ev.add("[BREACH] "+sevTag()+" Credentials submitted to attacker server");
                if(severityLevel>=7) ev.add("[MFA-BYPASS] Evilginx real-time OTP intercept");
                if(severityLevel>=8) ev.add("[O365] Attacker authenticated from 185.x.x.x in 47 seconds");
                if(severityLevel>=9) ev.add("[BEC] Forged wire transfer request sent internally");
                det.assessEvent("Phishing BREACH ["+lbl+"] — "+targetEmail,severityLevel);
            } else {
                ev.add("[BLOCKED] Time-of-click sandbox flagged domain — page blocked");
                ev.add("[ALERT] SOC notified; user warned via out-of-band call");
            }
            List<String> strong=Arrays.asList("Time-of-click URL sandbox","DMARC/DKIM/SPF enforced",
                "MFA on email accounts","Bi-annual phishing simulations");
            List<String> weak=severityLevel>=7
                ? Arrays.asList("SMS MFA (vulnerable to Evilginx)","No FIDO2 for finance staff",
                    "No external email warning banner","<24h domain registrations slip through",
                    "Weak incident reporting culture")
                : Arrays.asList("No external email warning banner","<24h domain registrations slip through");
            int health=blocked?scaledHealth(90,22,severityLevel):scaledHealth(20,5,severityLevel);
            String sum=blocked
                ?"["+lbl+"] Phishing blocked at time-of-click. Upgrade MFA to FIDO2 hardware tokens."
                :"["+lbl+"] BREACH: Creds harvested"+(severityLevel>=9?" + BEC":"")+". Reset creds, revoke sessions, notify finance.";
            return new AttackResult(!blocked,health,severityLevel,ev,strong,weak,type.displayName,sum,init);
        }
    }

    // ── Brute Force ───────────────────────────────────────────────────────────
    public static class BruteForceAttack extends Attack {
        private final String account; private final int threshold;
        public BruteForceAttack(String id,int sev,String acc,int thr){
            super(id,sev,AttackType.BRUTE_FORCE); account=acc; threshold=thr;
        }
        @Override public AttackResult executeAttack(Firewall fw,ThreatDetector det,String init){
            String lbl=severityLabel(severityLevel); List<String> ev=new ArrayList<>();
            ev.add("[INIT] "+sevTag()+" Credential stuffing — target: "+account);
            ev.add("[SRC] 847M leaked pairs from HaveIBeenPwned corpus");
            ev.add("[ATTEMPT-001] "+account+":password123 → 401");
            ev.add("[ATTEMPT-002] "+account+":Welcome1!   → 401");
            ev.add("[ATTEMPT-003] "+account+":Summer2024  → 401");
            if(severityLevel>=7) ev.add("[ROTATE] Rotating Tor exit node IPs to bypass per-IP lockout");
            ev.add("[FW] Burst threshold "+threshold+" failures/min — evaluating rule");
            boolean blocked=fw.filterTraffic("BRUTE_FORCE_AUTH",severityLevel);
            if(!blocked){
                ev.add("[ATTEMPT-247] "+account+":Welcome1 → 200 OK — AUTHENTICATED");
                ev.add("[BREACH] "+sevTag()+" Session established inside portal");
                if(severityLevel>=7) ev.add("[ESC] Account used to reset passwords for others");
                if(severityLevel>=8) ev.add("[PIVOT] Session token used to access internal admin");
                det.assessEvent("Brute Force BREACH ["+lbl+"] — "+account,severityLevel);
            } else {
                ev.add("[LOCKED] Account locked after "+threshold+" failures — 30 min lockout");
                ev.add("[CAPTCHA] Challenge triggered on /api/auth");
                if(severityLevel>=7) ev.add("[GEOBLOCK] Tor exit IPs added to deny list");
            }
            List<String> strong=Arrays.asList("Account lockout: "+threshold+" attempts / 30 min",
                "CAPTCHA on login","IP rate-limit on /api/auth","Geo-block on Tor/VPN ranges");
            List<String> weak=severityLevel>=7
                ? Arrays.asList("Legacy /admin lacks rate-limiting","Per-IP lockout (not per-account) bypassed by distributed IPs",
                    "Password policy allows <10 chars","No HIBP check on login","Slow-and-low undetected")
                : Arrays.asList("Per-IP lockout bypassed by distributed IPs","Password policy allows <10 chars");
            int health=blocked?scaledHealth(86,16,severityLevel):scaledHealth(16,3,severityLevel);
            String sum=blocked
                ?"["+lbl+"] Brute force blocked. Distributed slow-and-low still risky. Add HIBP + per-account lockout."
                :"["+lbl+"] BREACH: Credentials cracked. Force reset all accounts, revoke sessions, deploy MFA.";
            return new AttackResult(!blocked,health,severityLevel,ev,strong,weak,type.displayName,sum,init);
        }
    }

    // ── Zero-Day ──────────────────────────────────────────────────────────────
    public static class ZeroDayAttack extends Attack {
        private final String cveId; private final String component;
        public ZeroDayAttack(String id,int sev,String cve,String comp){
            super(id,sev,AttackType.ZERO_DAY); cveId=cve; component=comp;
        }
        @Override public AttackResult executeAttack(Firewall fw,ThreatDetector det,String init){
            String lbl=severityLabel(severityLevel); List<String> ev=new ArrayList<>();
            ev.add("[RECON] "+sevTag()+" Undisclosed vulnerability identified in "+component);
            ev.add("[CVE] "+cveId+" — no public patch available at time of exploit");
            ev.add("[CRAFT] Custom exploit shellcode crafted for architecture");
            if(severityLevel>=6) ev.add("[DELIVER] Exploit delivered via spear-phish attachment");
            if(severityLevel>=7) ev.add("[EXEC] Shellcode execution in unprivileged context");
            if(severityLevel>=8) ev.add("[PRIV-ESC] Kernel exploit: elevated to SYSTEM/root");
            if(severityLevel>=9) ev.add("[PERSIST] Rootkit installed — survives reboots");
            ev.add("[HEURISTIC] EDR anomaly engine flagging unusual process chain");
            boolean blocked=fw.filterTraffic("DDOS_FLOOD",Math.max(1,severityLevel-3));// partial FW aid
            if(!blocked){
                ev.add("[BREACH] "+sevTag()+" Exploit executed — RCE achieved on "+component);
                if(severityLevel>=8) ev.add("[EXFIL] Sensitive data exfiltrated via encrypted C2 tunnel");
                if(severityLevel>=9) ev.add("[NATION-STATE] APT lateral movement across domain");
                det.assessEvent("Zero-Day BREACH ["+lbl+"] CVE="+cveId,severityLevel);
            } else {
                ev.add("[PARTIAL] Firewall rate-limited delivery vector — exploit delayed");
                ev.add("[HEURISTIC] EDR process-kill on suspicious shellcode pattern");
                ev.add("[PATCH-MGMT] Virtual patch deployed via WAF rule as interim measure");
            }
            List<String> strong=Arrays.asList("EDR with heuristic engine","WAF virtual patching","Network segmentation",
                "Threat intelligence feed active");
            List<String> weak=Arrays.asList("No patch for "+cveId+" — vendor notified","SBOM not maintained",
                "Vulnerability disclosure programme absent","Egress DLP insufficient for exfil detection");
            int health=blocked?scaledHealth(60,10,severityLevel):scaledHealth(10,1,severityLevel);
            String sum=blocked
                ?"["+lbl+"] Zero-Day partially mitigated by EDR heuristics. Vendor patch pending — apply virtual WAF rule."
                :"["+lbl+"] CRITICAL BREACH: RCE via "+cveId+". Isolate system, engage IR team, notify vendor immediately.";
            return new AttackResult(!blocked,health,severityLevel,ev,strong,weak,type.displayName,sum,init);
        }
    }

    // ── AuthenticationManager ──────────────────────────────────────────────────
    public static class AuthenticationManager {
        private final Map<String,User> registry;
        private final List<String>     audit;
        private User activeSession;

        public AuthenticationManager(){
            registry=new LinkedHashMap<>();
            audit=Collections.synchronizedList(new ArrayList<>());
            seedUsers();
        }
        private void seedUsers(){
            // Admin: full powers
            registry.put("dr.carter", new Admin("USR-001","dr.carter",
                hashPassword("Nexus@SecOps#9"),
                "dr.carter@cttms.gov"));
            // Senior Analyst
            registry.put("r.hayes", new SecurityAnalyst("USR-002","r.hayes",
                hashPassword("Hawk3ye$Threat!"),
                "r.hayes@cttms.gov","TOP_SECRET//SCI"));
            // Junior Analyst
            registry.put("m.okonkwo", new SecurityAnalyst("USR-003","m.okonkwo",
                hashPassword("Cipher$0Sigma"),
                "m.okonkwo@cttms.gov","SECRET"));
            // Trainee
            registry.put("j.novak", new Trainee("USR-004","j.novak",
                hashPassword("Tr4inee!Cyber24"),
                "j.novak@cttms.gov"));
        }
        public User authenticate(String username,String password){
            User u=registry.get(username.toLowerCase());
            if (u!=null&&u.login(username.toLowerCase(),password)){
                activeSession=u;
                audit.add("["+timestamp()+"] LOGIN_SUCCESS user="+username+" role="+u.getRole());
                return u;
            }
            audit.add("["+timestamp()+"] LOGIN_FAILURE user="+username);
            return null;
        }
        public void endSession(){
            if(activeSession!=null){
                audit.add("["+timestamp()+"] LOGOUT user="+activeSession.getUsername());
                activeSession.logout();
            }
            activeSession=null;
        }
        public boolean registerUser(String username,String password,UserRole role,String email){
            if(username==null||username.trim().isEmpty()) return false;
            if(registry.containsKey(username.toLowerCase())) return false;
            if(password==null||password.length()<8) return false;
            String id="USR-"+String.format("%03d",registry.size()+1);
            String mail=(email==null||email.isEmpty())?username.toLowerCase()+"@cttms.gov":email;
            User u; switch(role){
                case ADMIN:   u=new Admin(id,username.toLowerCase(),hashPassword(password),mail); break;
                case ANALYST: u=new SecurityAnalyst(id,username.toLowerCase(),hashPassword(password),mail,"CONFIDENTIAL"); break;
                default:      u=new Trainee(id,username.toLowerCase(),hashPassword(password),mail); break;
            }
            registry.put(username.toLowerCase(),u);
            audit.add("["+timestamp()+"] USER_CREATED id="+id+" role="+role);
            return true;
        }
        public boolean deleteUser(String username){
            if(username.equals("dr.carter")) return false;
            if(activeSession!=null&&activeSession.getUsername().equals(username)) return false;
            boolean ok=registry.remove(username.toLowerCase())!=null;
            if(ok) audit.add("["+timestamp()+"] USER_DELETED username="+username);
            return ok;
        }
        public boolean changePassword(String username,String oldPass,String newPass){
            User u=registry.get(username.toLowerCase());
            if(u==null||!verifyPassword(oldPass,u.passwordHash)) return false;
            if(newPass==null||newPass.length()<8) return false;
            u.passwordHash=hashPassword(newPass);
            audit.add("["+timestamp()+"] PASSWORD_CHANGED username="+username);
            return true;
        }
        public User             getActiveSession()  { return activeSession; }
        public Collection<User> getAllUsers()        { return registry.values(); }
        public List<String>     getAuditTrail()     { return Collections.unmodifiableList(audit); }
        public int              getUserCount()       { return registry.size(); }
    }

    // ── SimulationController ───────────────────────────────────────────────────
    public static class SimulationController {
        private final Firewall       firewall;
        private final ThreatDetector detector;
        private final SecurityLog    securityLog;
        private final List<AttackResult> history;

        public SimulationController(Firewall fw,ThreatDetector det,SecurityLog log){
            firewall=fw; detector=det; securityLog=log;
            history=Collections.synchronizedList(new ArrayList<>());
        }
        public Attack buildAttack(AttackType type,int sev){
            String id=type.name().substring(0,2).toUpperCase()+"-"+(100+new Random().nextInt(900));
            Random rnd=new Random();
            switch(type){
                case DDOS:
                    return new DDoSAttack(id,sev,50000+rnd.nextInt(50000),
                        "192.168."+rnd.nextInt(10)+"."+(rnd.nextInt(254)+1));
                case MITM:
                    return new MITMAttack(id,sev,"10.0."+rnd.nextInt(5)+".0/24",
                        "10.0."+rnd.nextInt(5)+"."+(rnd.nextInt(200)+10));
                case SQL_INJECTION:
                    return new SQLInjectionAttack(id,sev,"users",
                        "' OR '1'='1' UNION SELECT null,username,password,null FROM admin--");
                case RANSOMWARE:
                    return new RansomwareAttack(id,sev,sha256("KEY_"+id).substring(0,16),
                        1800+rnd.nextInt(3000));
                case PHISHING:
                    return new PhishingAttack(id,sev,"finance.director@corp.internal",
                        "corp-internal-verify.net");
                case BRUTE_FORCE:
                    return new BruteForceAttack(id,sev,"admin_portal",3);
                case ZERO_DAY:
                    return new ZeroDayAttack(id,sev,
                        "CVE-2025-"+String.format("%04d",1000+rnd.nextInt(9000)),
                        rnd.nextBoolean()?"Apache HTTP Server":"OpenSSH Daemon");
                default: throw new IllegalArgumentException("Unknown: "+type);
            }
        }
        public AttackResult simulate(AttackType type,int sev,String initiator){
            securityLog.writeLog("INFO","Sim START: "+type.displayName
                +" | Sev="+sev+"["+severityLabel(sev)+"] | by="+initiator);
            Attack a=buildAttack(type,sev);
            AttackResult r=a.executeAttack(firewall,detector,initiator);
            a.logAttackDetails(securityLog);
            securityLog.writeLog(r.breached?"CRITICAL":"WARNING",
                "Sim END: "+type.displayName+" | Breached="+r.breached
                    +" | Health="+r.finalHealth+"% | Sev="+sev+"["+severityLabel(sev)+"]");
            history.add(r); return r;
        }
        public List<AttackResult> getResultHistory(){ return Collections.unmodifiableList(history); }
        public Firewall       getFirewall()          { return firewall; }
        public ThreatDetector getDetector()          { return detector; }
        public SecurityLog    getSecurityLog()       { return securityLog; }
    }

    // ── SystemBootstrap ────────────────────────────────────────────────────────
    public static class SystemBootstrap {
        private final AuthenticationManager authManager;
        private final SimulationController  simCtrl;
        private final SecurityLog           securityLog;
        public SystemBootstrap(){
            securityLog = new SecurityLog("logs/security_audit.log");
            Firewall fw = new Firewall(5);
            ThreatDetector det = new ThreatDetector();
            authManager = new AuthenticationManager();
            simCtrl     = new SimulationController(fw,det,securityLog);
            securityLog.writeLog("INFO","CTTMS v"+VERSION+" initialised — all subsystems online");
        }
        public AuthenticationManager getAuthManager()         { return authManager; }
        public SimulationController  getSimulationController(){ return simCtrl; }
        public SecurityLog           getSecurityLog()         { return securityLog; }
    }
}
