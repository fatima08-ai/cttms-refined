import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.text.*;

public class CyberDashboardViewer {
    // ── Cached strokes ─────────────────────────────────────────────────────────
    static final BasicStroke S1   = new BasicStroke(1f);
    static final BasicStroke S1H  = new BasicStroke(1.5f);
    static final BasicStroke S2   = new BasicStroke(2f);
    static final BasicStroke S2H  = new BasicStroke(2.5f);
    static final BasicStroke DASH = new BasicStroke(1f,BasicStroke.CAP_BUTT,
        BasicStroke.JOIN_MITER,10,new float[]{6,5},0);

    // ── Fonts ──────────────────────────────────────────────────────────────────
    static final Font MONO_LG  = new Font("Monospaced",Font.BOLD,13);
    static final Font MONO_MD  = new Font("Monospaced",Font.PLAIN,12);
    static final Font MONO_SM  = new Font("Monospaced",Font.PLAIN,11);
    static final Font MONO_XS  = new Font("Monospaced",Font.PLAIN,10);
    static final Font LABEL_F  = new Font("SansSerif",Font.BOLD,12);

    // ── Colour shortcuts from ThemeConstants ───────────────────────────────────
    static Color BG()    { return AppCoordinator.ThemeConstants.bgDeep();   }
    static Color PANEL() { return AppCoordinator.ThemeConstants.bgPanel();  }
    static Color CARD()  { return AppCoordinator.ThemeConstants.bgCard();   }
    static Color INPUT() { return AppCoordinator.ThemeConstants.bgInput();  }
    static Color TXT()   { return AppCoordinator.ThemeConstants.textPri();  }
    static Color DIM()   { return AppCoordinator.ThemeConstants.textDim();  }
    static Color BRT()   { return AppCoordinator.ThemeConstants.textBrt();  }
    static Color GREEN() { return AppCoordinator.ThemeConstants.NEON_GREEN; }
    static Color BLUE()  { return AppCoordinator.ThemeConstants.NEON_BLUE;  }
    static Color CYAN()  { return AppCoordinator.ThemeConstants.NEON_CYAN;  }
    static Color PURP()  { return AppCoordinator.ThemeConstants.NEON_PURPLE;}
    static Color RED()   { return AppCoordinator.ThemeConstants.ALERT_RED;  }
    static Color ORAN()  { return AppCoordinator.ThemeConstants.ALERT_ORANGE;}
    static Color YELL()  { return AppCoordinator.ThemeConstants.WARN_YELLOW;}
    static Color MUTE()  { return AppCoordinator.ThemeConstants.MUTED_GRAY; }

    // ── Instance ───────────────────────────────────────────────────────────────
    private JFrame               mainFrame;
    private JPanel               cardContainer;
    private CardLayout           cardLayout;
    private LoginPanel           loginPanel;
    private DashboardPanel       dashboardPanel;
    private BackendEngine.SystemBootstrap backend;
    private User   currentUser;

    public CyberDashboardViewer(BackendEngine.SystemBootstrap be){ backend=be; }

    public void launch(){
        SwingUtilities.invokeLater(()->{
            try{ UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
            catch(Exception ignored){}
            buildFrame();
        });
    }
    private void buildFrame(){
        mainFrame=new JFrame("CTTMS — Cybersecurity Training & Threat Monitoring  v"+BackendEngine.VERSION);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setMinimumSize(new Dimension(1380,780));
        mainFrame.setPreferredSize(new Dimension(1640,960));
        cardLayout=new CardLayout(); cardContainer=new JPanel(cardLayout);
        cardContainer.setBackground(BG());
        loginPanel    =new LoginPanel(this);
        dashboardPanel=new DashboardPanel(this);
        cardContainer.add(loginPanel,"LOGIN");
        cardContainer.add(dashboardPanel,"DASHBOARD");
        mainFrame.setContentPane(cardContainer);
        mainFrame.pack(); mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
        cardLayout.show(cardContainer,"LOGIN");
    }
    public void onLoginSuccess(User user) {
            this.currentUser = user;
            System.out.println("Login validated for: " + user.getUsername());
            
            try {
                // 1. Initialize the dashboard layout with user permissions
                if (this.dashboardPanel != null) {
                    this.dashboardPanel.initForUser(user); 
                }
                
                // 2. HARD BYPASS: If CardLayout is stuck, rip out everything and force add the dashboard
                if (this.mainFrame != null && this.dashboardPanel != null) {
                    System.out.println("[DIAGNOSTIC] Hard-swapping content pane to dashboardPanel...");
                    this.mainFrame.getContentPane().removeAll();
                    this.mainFrame.getContentPane().add(this.dashboardPanel);
                    this.mainFrame.getContentPane().revalidate();
                    this.mainFrame.getContentPane().repaint();
                    return; // Exit method early since we bypassed cardLayout
                }
            } catch (Exception e) {
                System.err.println("❌ ERROR DURING DASHBOARD MAIN INITIALIZATION:");
                e.printStackTrace();
            }
    
            // Fallback card layout switch if mainFrame isn't assigned directly
            if (this.cardLayout != null && this.cardContainer != null) {
                this.cardLayout.next(this.cardContainer);
                this.cardContainer.revalidate();
                this.cardContainer.repaint();
            }
        }
    void onLogout(){
        backend.getAuthManager().endSession(); currentUser=null;
        loginPanel.reset(); cardLayout.show(cardContainer,"LOGIN");
    }
    void toggleTheme(){
        boolean nowDark=!AppCoordinator.ThemeConstants.isDark();
        AppCoordinator.applyUIDefaults(nowDark);
        // Rebuild UI on EDT
        SwingUtilities.invokeLater(()->{
            cardContainer.setBackground(BG());
            loginPanel.repaint(); dashboardPanel.repaint();
            SwingUtilities.updateComponentTreeUI(mainFrame);
            mainFrame.repaint();
        });
    }
    BackendEngine.SystemBootstrap getBackend()   { return backend; }
    public User getCurrentUser() {
    return this.currentUser;
}

    JFrame                        getMainFrame()  { return mainFrame; }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Shared UI factories
    // ═══════════════════════════════════════════════════════════════════════════
    static JPanel card(String title,Color accent){
        JPanel p=new JPanel(new BorderLayout());
        p.setBackground(CARD());
        p.setBorder(new CompoundBorder(new GlowBorder(accent,1),new EmptyBorder(10,12,10,12)));
        if(title!=null&&!title.isEmpty()){
            JLabel l=new JLabel("▸  "+title);l.setFont(LABEL_F);l.setForeground(accent);
            l.setBorder(new EmptyBorder(0,0,6,0));p.add(l,BorderLayout.NORTH);
        }
        return p;
    }
    static JButton cyberBtn(String text,Color fg,Color bg){
        JButton b=new JButton(text){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                Color fill=getModel().isPressed()?bg.darker().darker():getModel().isRollover()?bg.brighter():bg;
                g2.setColor(fill);g2.fillRoundRect(0,0,getWidth(),getHeight(),7,7);
                g2.setColor(fg);g2.setFont(getFont());
                FontMetrics fm=g2.getFontMetrics();
                g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,
                    (getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        b.setFont(LABEL_F);b.setForeground(fg);b.setBackground(bg);
        b.setBorderPainted(false);b.setFocusPainted(false);b.setContentAreaFilled(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(158,34));
        return b;
    }
    static JTextField styledField(Color accent){
        JTextField f=new JTextField();
        f.setBackground(INPUT());f.setForeground(GREEN());f.setCaretColor(GREEN());
        f.setFont(MONO_MD);f.setOpaque(true);
        f.setBorder(new CompoundBorder(new GlowBorder(accent,1),new EmptyBorder(7,10,7,10)));
        return f;
    }
    static JPasswordField styledPass(Color accent){
        JPasswordField f=new JPasswordField();
        f.setBackground(INPUT());f.setForeground(GREEN());f.setCaretColor(GREEN());
        f.setFont(MONO_MD);f.setOpaque(true);
        f.setBorder(new CompoundBorder(new GlowBorder(accent,1),new EmptyBorder(7,10,7,10)));
        return f;
    }
    static void styleScroll(JScrollPane sp,Color accent){
        sp.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI(){
            @Override protected void configureScrollBarColors(){ thumbColor=accent;trackColor=CARD(); }
            @Override protected JButton createDecreaseButton(int o){ JButton b=new JButton();b.setPreferredSize(new Dimension(0,0));return b; }
            @Override protected JButton createIncreaseButton(int o){ JButton b=new JButton();b.setPreferredSize(new Dimension(0,0));return b; }
        });
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  GlowBorder
    // ═══════════════════════════════════════════════════════════════════════════
    static class GlowBorder extends AbstractBorder {
        private final Color c; private final int t;
        GlowBorder(Color c,int t){ this.c=c; this.t=t; }
        @Override public void paintBorder(Component comp,Graphics g,int x,int y,int w,int h){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),50));
            g2.setStroke(new BasicStroke(t+2));g2.drawRoundRect(x+1,y+1,w-3,h-3,5,5);
            g2.setColor(c);g2.setStroke(new BasicStroke(t));g2.drawRoundRect(x,y,w-1,h-1,5,5);
            g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c){ return new Insets(t+3,t+3,t+3,t+3); }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  MatrixRain  (login background)
    // ═══════════════════════════════════════════════════════════════════════════
    static class MatrixRain {
        private final char[][]  grid;
        private final float[]   speeds,offsets;
        private final Color[]   cols;
        private static final int C=100,R=55,CW=13,CH=15;
        private static final char[] CS="01アイウエオカキクABCDEFGHIJKLMNOPQRSTUVWXYZ#$%&@!".toCharArray();
        private final Random    rnd=new Random();
        private final Timer     timer;
        private final JComponent owner;
        private final Font      mFont=new Font("Monospaced",Font.PLAIN,11);
        MatrixRain(JComponent owner){
            this.owner=owner; grid=new char[C][R]; speeds=new float[C]; offsets=new float[C]; cols=new Color[C];
            for(int c=0;c<C;c++){
                speeds[c]=0.2f+rnd.nextFloat()*0.7f; offsets[c]=rnd.nextFloat()*R;
                cols[c]=rnd.nextBoolean()?GREEN():BLUE();
                for(int r=0;r<R;r++) grid[c][r]=CS[rnd.nextInt(CS.length)];
            }
            timer=new Timer(55,e->{ tick(); owner.repaint(); }); timer.start();
        }
        private void tick(){
            for(int c=0;c<C;c++){
                offsets[c]+=speeds[c]; if(offsets[c]>R) offsets[c]=-rnd.nextInt(R/2);
                if(rnd.nextInt(8)==0) grid[c][rnd.nextInt(R)]=CS[rnd.nextInt(CS.length)];
            }
        }
        void paint(Graphics g,int w,int h){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(mFont);
            for(int c=0;c<C&&c*CW<w;c++){
                int head=(int)offsets[c];
                for(int r=0;r<R&&r*CH<h;r++){
                    float dist=Math.abs(r-head); if(dist>20) continue;
                    float alpha=Math.max(0f,1f-dist/20f);
                    Color base=cols[c];
                    Color col=r==head
                        ? new Color(200,255,220,(int)(alpha*255))
                        : new Color(base.getRed(),base.getGreen(),base.getBlue(),(int)(alpha*145));
                    g2.setColor(col);
                    g2.drawString(String.valueOf(grid[c][r]),c*CW,r*CH+CH);
                }
            }
            g2.dispose();
        }
        void stop(){ timer.stop(); }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  TerminalPane
    // ═══════════════════════════════════════════════════════════════════════════
    static class TerminalPane extends JTextPane {
        TerminalPane(){
            setBackground(AppCoordinator.ThemeConstants.isDark()?new Color(6,9,15):new Color(240,245,255));
            setForeground(GREEN()); setFont(MONO_MD); setEditable(false);
            setOpaque(true); setBorder(new EmptyBorder(8,10,8,10)); setCaretColor(GREEN());
        }
        void appendLine(String text,Color color){
            SwingUtilities.invokeLater(()->{
                StyledDocument doc=getStyledDocument();
                Style s=addStyle("s"+System.nanoTime(),null);
                StyleConstants.setForeground(s,color);
                StyleConstants.setFontFamily(s,"Monospaced"); StyleConstants.setFontSize(s,12);
                try{ doc.insertString(doc.getLength(),text+"\n",s); setCaretPosition(doc.getLength()); }
                catch(Exception ignored){}
            });
        }
        void clear(){ SwingUtilities.invokeLater(()->setText("")); }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  StatCard
    // ═══════════════════════════════════════════════════════════════════════════
    static class StatCard extends JPanel {
        private final JLabel valLbl;
        StatCard(String title,String val,Color accent){
            setBackground(CARD()); setBorder(new CompoundBorder(new GlowBorder(accent,1),new EmptyBorder(10,14,10,14)));
            setLayout(new BorderLayout(2,2));
            JLabel t=new JLabel(title); t.setFont(MONO_XS); t.setForeground(DIM());
            valLbl=new JLabel(val); valLbl.setFont(new Font("Monospaced",Font.BOLD,22)); valLbl.setForeground(accent);
            add(t,BorderLayout.NORTH); add(valLbl,BorderLayout.CENTER);
        }
        void set(String v){ SwingUtilities.invokeLater(()->valLbl.setText(v)); }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  PulsingDot
    // ═══════════════════════════════════════════════════════════════════════════
    static class PulsingDot extends JPanel {
        private float alpha=1f; private boolean grow=false; private final Color color;
        PulsingDot(Color c){ color=c; setOpaque(false); setPreferredSize(new Dimension(10,10));
            new Timer(55,e->{ alpha+=grow?0.07f:-0.07f;
                if(alpha>=1f){alpha=1f;grow=false;} if(alpha<=0.15f){alpha=0.15f;grow=true;}
                repaint(); }).start();
        }
        @Override protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(color.getRed(),color.getGreen(),color.getBlue(),(int)(alpha*255)));
            g2.fillOval(0,1,9,9); g2.dispose();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  AttackAnimPanel  — unique visuals per attack type + severity-reactive
    // ═══════════════════════════════════════════════════════════════════════════
    static class AttackAnimPanel extends JPanel {
        private Timer    animTimer;
        private float    progress=0f, shield=1f;
        private boolean  breached=false, active=false, done=false;
        private int      tick=0, currentSeverity=5;
        private BackendEngine.AttackType currentType;
        private Runnable onComplete;
        private final Random rnd=new Random();

        // DDoS state
        private final List<float[]> ddosNodes=new ArrayList<>();
        // MITM state
        private float mitmAX=0f, interceptAlpha=0f; private boolean mitmPos=false;
        // SQLi state
        private final List<String> sqlTokens=new ArrayList<>(); private int sqlIdx=0;
        // Ransomware state
        private int[] encFiles; private static final int FC=20;
        // Phishing state
        private float emailProg=0f; private boolean clicked=false;
        // Brute Force state
        private int bfAttempt=0;
        // Zero-Day state
        private float zdProbe=0f; private int zdStage=0;

        // Severity palette (set in start())
        private Color sevA, sevB, sevC;
        private int   timerMs, nodeCount;

        AttackAnimPanel(){ setBackground(BG()); setMinimumSize(new Dimension(420,210)); }

        private void buildSeverityProfile(int sev){
            currentSeverity=sev;
            if(sev<=3){    sevA=GREEN(); sevB=CYAN();  sevC=BLUE();  timerMs=58; nodeCount=9;  }
            else if(sev<=5){sevA=YELL(); sevB=ORAN();  sevC=YELL();  timerMs=48; nodeCount=13; }
            else if(sev<=7){sevA=ORAN(); sevB=RED();   sevC=ORAN();  timerMs=38; nodeCount=18; }
            else {          sevA=RED();  sevB=new Color(255,0,180); sevC=new Color(255,60,60); timerMs=28; nodeCount=26; }
        }

        void start(BackendEngine.AttackType type,boolean breach,int sev,Runnable cb){
            if(animTimer!=null) animTimer.stop();
            this.currentType=type; this.breached=breach; this.onComplete=cb;
            progress=0f; shield=1f; active=true; done=false; tick=0;
            mitmPos=false; mitmAX=0f; interceptAlpha=0f;
            emailProg=0f; clicked=false; bfAttempt=0; sqlIdx=0; zdProbe=0f; zdStage=0;
            ddosNodes.clear(); sqlTokens.clear(); encFiles=new int[FC];
            buildSeverityProfile(sev);
            if(type==BackendEngine.AttackType.DDOS)
                for(int i=0;i<nodeCount;i++)
                    ddosNodes.add(new float[]{rnd.nextFloat(),0.06f+rnd.nextFloat()*0.88f,
                        rnd.nextFloat(),1.5f+rnd.nextFloat()*2.5f});
            if(type==BackendEngine.AttackType.SQL_INJECTION)
                sqlTokens.addAll(Arrays.asList("'","OR","1=1","UNION","SELECT","*","FROM","users","--","NULL","DROP"));
            animTimer=new Timer(timerMs,e->{ tickAnim(); repaint(); }); animTimer.start();
        }

        private void tickAnim(){
            tick++; progress=Math.min(1f,progress+0.011f);
            if(tick>22) shield=Math.max(breached?0.03f:0.30f,shield-0.017f);
            // Type-specific state
            if(currentType==BackendEngine.AttackType.BRUTE_FORCE&&tick%10==0) bfAttempt++;
            if(currentType==BackendEngine.AttackType.MITM){
                if(!mitmPos) mitmAX=Math.min(0.5f,mitmAX+0.014f);
                if(mitmAX>=0.47f){ mitmPos=true; interceptAlpha=Math.min(1f,interceptAlpha+0.038f); }
            }
            if(currentType==BackendEngine.AttackType.PHISHING){
                emailProg=Math.min(1f,emailProg+0.014f);
                if(emailProg>0.58f&&!clicked&&breached) clicked=true;
            }
            if(currentType==BackendEngine.AttackType.RANSOMWARE&&tick%Math.max(1,5-currentSeverity/3)==0){
                int idx=rnd.nextInt(FC); encFiles[idx]=(!breached&&tick>48)?2:1;
            }
            if(currentType==BackendEngine.AttackType.SQL_INJECTION&&tick%14==0&&sqlIdx<sqlTokens.size()) sqlIdx++;
            if(currentType==BackendEngine.AttackType.ZERO_DAY){
                zdProbe=Math.min(1f,zdProbe+0.009f);
                if(tick%30==0&&zdStage<5) zdStage++;
            }
            if(tick>125&&!done){ done=true; active=false; animTimer.stop();
                if(onComplete!=null) SwingUtilities.invokeLater(onComplete); }
        }

        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
            int w=getWidth(),h=getHeight();
            // Background
            g2.setColor(BG()); g2.fillRect(0,0,w,h);
            // Severity pulse overlay for high/critical
            if(currentSeverity>=7&&active){
                float pulse=(float)(0.5+0.5*Math.sin(tick*0.22));
                int a=(int)(18+pulse*28*(currentSeverity-6));
                g2.setColor(new Color(sevA.getRed(),sevA.getGreen(),sevA.getBlue(),Math.min(a,75)));
                g2.fillRect(0,0,w,h);
            }
            if(currentType==null){ g2.dispose(); return; }
            switch(currentType){
                case DDOS:          drawDDoS(g2,w,h);       break;
                case MITM:          drawMITM(g2,w,h);       break;
                case SQL_INJECTION: drawSQLi(g2,w,h);       break;
                case RANSOMWARE:    drawRansomware(g2,w,h); break;
                case PHISHING:      drawPhishing(g2,w,h);   break;
                case BRUTE_FORCE:   drawBruteForce(g2,w,h); break;
                case ZERO_DAY:      drawZeroDay(g2,w,h);    break;
            }
            drawStatusBar(g2,w,h);
            g2.dispose();
        }

        // ── DDoS: concentric botnet rings + flood waves ────────────────────────
        private void drawDDoS(Graphics2D g2,int w,int h){
            int tX=(int)(w*0.80f),tY=h/2,tR=38;
            // Botnet mesh lines
            g2.setColor(new Color(sevA.getRed(),sevA.getGreen(),sevA.getBlue(),22));
            g2.setStroke(new BasicStroke(0.5f));
            for(int i=0;i<ddosNodes.size()-1;i++){
                float[]a=ddosNodes.get(i),b=ddosNodes.get((i+1)%ddosNodes.size());
                g2.drawLine((int)(w*0.04f+w*0.16f*a[0]),(int)(h*a[1]),
                    (int)(w*0.04f+w*0.16f*b[0]),(int)(h*b[1]));
            }
            for(float[] n:ddosNodes){
                int nx=(int)(w*0.04f+w*0.16f*n[0]),ny=(int)(h*n[1]);
                // Glow halo
                g2.setColor(new Color(sevA.getRed(),sevA.getGreen(),sevA.getBlue(),22));
                g2.fillOval(nx-14,ny-14,28,28);
                g2.setColor(new Color(sevA.getRed(),sevA.getGreen(),sevA.getBlue(),130));
                g2.fillOval(nx-7,ny-7,14,14);
                g2.setColor(sevA); g2.setStroke(S1); g2.drawOval(nx-7,ny-7,14,14);
                // Packets flying to target
                float pt=(progress+n[2])%1.0f;
                if(pt<0.93f){
                    float px=nx+(tX-nx)*pt,py=ny+(tY-ny)*pt;
                    float al=pt<0.1f?pt*10f:pt>0.83f?(1f-pt)*6f:1f;
                    // Different packet shapes per severity
                    int ps=(int)n[3];
                    g2.setColor(new Color(sevA.getRed(),sevA.getGreen(),sevA.getBlue(),(int)(al*210)));
                    if(currentSeverity>=8) g2.fillRect((int)px-ps/2,(int)py-ps/2,ps,ps);
                    else g2.fillOval((int)px-ps/2,(int)py-ps/2,ps,ps);
                }
            }
            nodeLabel(g2,(int)(w*0.10f),h/2,"BOTNET",nodeCount+" NODES",sevA);
            shieldNode(g2,tX,tY,tR,shield,"TARGET","SERVER");
            healthBar(g2,tX-52,tY+tR+14,104,9,shield);
            // Wave label
            g2.setFont(MONO_XS); g2.setColor(sevB);
            g2.drawString((currentSeverity>=8?"VOLUMETRIC":currentSeverity>=5?"SYN FLOOD":"UDP FLOOD")
                +" ATTACK",tX-60,tY-tR-18);
        }

        // ── MITM: victim ↔ attacker ↔ server interception ─────────────────────
        private void drawMITM(Graphics2D g2,int w,int h){
            int cX=(int)(w*0.10f),cY=h/2,sX=(int)(w*0.90f),sY=h/2;
            int aX=(int)(mitmAX*w+w*0.20f),aY=h/2-70;
            // Baseline dotted channel
            g2.setColor(new Color(80,80,80,55)); g2.setStroke(DASH);
            g2.drawLine(cX,cY,sX,sY);
            simpleNode(g2,cX,cY,26,CYAN(),"CLIENT","");
            shieldNode(g2,sX,sY,28,shield,"SERVER","");
            healthBar(g2,sX-46,sY+44,92,8,shield);
            if(mitmAX>0.01f){
                Color ac=mitmPos?sevA:sevB;
                simpleNode(g2,aX,aY,26,ac,"ATTACKER","");
                if(mitmPos){
                    // Intercept lines
                    g2.setColor(new Color(sevA.getRed(),sevA.getGreen(),sevA.getBlue(),(int)(interceptAlpha*160)));
                    g2.setStroke(S2);
                    g2.drawLine(cX,cY,aX,aY); g2.drawLine(aX,aY,sX,sY);
                    // Animated packet client→attacker→server
                    float pt=(progress*1.4f)%1.0f;
                    if(pt<0.48f){ float t2=pt/0.48f;
                        float px=cX+(aX-cX)*t2,py=cY+(aY-cY)*t2;
                        g2.setColor(new Color(255,220,0,200)); g2.fillOval((int)px-5,(int)py-5,10,10);
                    } else { float t2=(pt-0.48f)/0.52f;
                        float px=aX+(sX-aX)*t2,py=aY+(sY-aY)*t2;
                        g2.setColor(new Color(sevB.getRed(),sevB.getGreen(),sevB.getBlue(),200));
                        g2.fillOval((int)px-5,(int)py-5,10,10);
                    }
                    g2.setFont(MONO_XS);
                    g2.setColor(new Color(sevA.getRed(),sevA.getGreen(),sevA.getBlue(),(int)(interceptAlpha*200)));
                    g2.drawString("◀ ARP POISONED ▶",aX-48,aY-18);
                    if(interceptAlpha>0.6f){ g2.setColor(new Color(255,220,0,(int)(interceptAlpha*180)));
                        g2.drawString("DECRYPTING...",aX-42,aY+46); }
                    if(interceptAlpha>0.82f&&breached){ g2.setColor(sevA);
                        g2.drawString("SESSION HIJACKED",aX-52,aY+60); }
                    // TLS strip badge if high severity
                    if(currentSeverity>=7&&interceptAlpha>0.5f){
                        g2.setColor(new Color(255,180,0,(int)(interceptAlpha*160)));
                        g2.drawString("TLS-STRIP",aX-28,aY-32);
                    }
                } else {
                    g2.setColor(sevB); g2.setStroke(S1H);
                    g2.drawLine(cX+28,cY-8,aX,aY+28);
                    g2.setFont(MONO_XS); g2.setColor(sevB); g2.drawString("MOVING IN...",cX+34,cY-20);
                }
            }
        }

        // ── SQLi: attacker → webapp → database with token stream ─────────────
        private void drawSQLi(Graphics2D g2,int w,int h){
            int aX=(int)(w*0.08f),aY=h/2,appX=(int)(w*0.46f),appY=h/2,dX=(int)(w*0.80f),dY=h/2;
            simpleNode(g2,aX,aY,24,sevA,"ATTACKER","");
            simpleNode(g2,appX,appY,24,YELL(),"WEB APP","");
            shieldNode(g2,dX,dY,30,shield,"DATABASE","");
            healthBar(g2,dX-46,dY+46,92,8,shield);
            // Connection lines
            g2.setColor(new Color(170,170,60,55)); g2.setStroke(S1);
            g2.drawLine(aX+26,aY,appX-26,appY); g2.drawLine(appX+26,appY,dX-32,dY);
            // Flying SQL packet (arc)
            float pt1=(progress*1.3f)%1.0f;
            float px1=aX+(appX-aX)*Math.min(pt1*2,1f);
            float py1=aY+(float)Math.sin(pt1*Math.PI)*(-26f);
            g2.setColor(new Color(sevA.getRed(),sevA.getGreen(),sevA.getBlue(),190));
            g2.fillRect((int)px1-4,(int)py1-4,8,8);
            // SQL token stream
            g2.setFont(MONO_XS);
            for(int i=0;i<Math.min(sqlIdx,sqlTokens.size());i++){
                float t=(float)i/sqlTokens.size();
                float pxT=appX+(dX-appX)*Math.min((progress-t*0.3f)*2.2f,1f);
                float pyT=appY-30+i*13f;
                if(pxT>appX+10){
                    Color tc=i<4?sevB:sevA;
                    g2.setColor(new Color(tc.getRed(),tc.getGreen(),tc.getBlue(),175));
                    g2.drawString(sqlTokens.get(i),(int)pxT,(int)pyT);
                }
            }
            // WAF badge
            g2.setFont(MONO_XS);
            g2.setColor(new Color(BLUE().getRed(),BLUE().getGreen(),BLUE().getBlue(),140));
            g2.drawString("[WAF]",appX+30,appY-40);
        }

        // ── Ransomware: file grid encrypting cell-by-cell ─────────────────────
        private void drawRansomware(Graphics2D g2,int w,int h){
            int cols=10,rows=2,cW=36,cH=40;
            int sX=w/2-(cols*cW)/2,sY=h/2-rows*cH/2-10;
            for(int r=0;r<rows;r++) for(int c=0;c<cols;c++){
                int idx=r*cols+c,fx=sX+c*cW+2,fy=sY+r*cH+2,fw=cW-4,fh=cH-5;
                Color fc=idx<FC&&encFiles[idx]==1
                    ? new Color(sevA.getRed(),sevA.getGreen(),sevA.getBlue(),195)
                    : idx<FC&&encFiles[idx]==2?new Color(0,200,90,160):new Color(38,52,78);
                g2.setColor(fc); g2.fillRoundRect(fx,fy,fw,fh,4,4);
                g2.setColor(new Color(255,255,255,22)); g2.setStroke(S1); g2.drawRoundRect(fx,fy,fw,fh,4,4);
                if(idx<FC&&encFiles[idx]==1){
                    g2.setFont(new Font("Dialog",Font.BOLD,11)); g2.setColor(Color.WHITE);
                    g2.drawString("🔒",fx+fw/2-6,fy+fh/2+5);
                }
            }
            long enc=0; for(int f:encFiles) if(f==1) enc++;
            float pct=(float)enc/FC;
            // Progress bar
            g2.setColor(new Color(28,36,52)); g2.fillRoundRect(sX,sY+rows*cH+10,cols*cW,10,4,4);
            g2.setColor(sevA); g2.fillRoundRect(sX,sY+rows*cH+10,(int)(cols*cW*pct),10,4,4);
            g2.setFont(MONO_XS); g2.setColor(sevB);
            g2.drawString(String.format("AES-256: %.0f%%  (%d / %d files)",pct*100,enc,FC),
                sX,sY+rows*cH+34);
            // C2 demand label
            if(breached&&progress>0.82f){
                g2.setFont(new Font("Monospaced",Font.BOLD,13)); g2.setColor(sevA);
                double btc=0.5+(currentSeverity-1)*0.5;
                g2.drawString(String.format("README_LOCKED.txt — DEMAND: %.1f BTC",btc),sX,sY-16);
            }
            if(!breached&&progress>0.82f){
                g2.setFont(new Font("Monospaced",Font.BOLD,12)); g2.setColor(GREEN());
                g2.drawString("EDR BLOCKED — ROLLBACK INITIATED",sX,sY-16);
            }
        }

        // ── Phishing: animated email envelope arcing toward victim ─────────────
        private void drawPhishing(Graphics2D g2,int w,int h){
            int vX=(int)(w*0.78f),vY=h/2,aX=(int)(w*0.12f),aY=h/2;
            simpleNode(g2,aX,aY,26,sevA,"ATTACKER","");
            shieldNode(g2,vX,vY,30,shield,"VICTIM","");
            healthBar(g2,vX-46,vY+44,92,8,shield);
            // Dotted baseline
            g2.setColor(new Color(100,100,100,40)); g2.setStroke(DASH);
            g2.drawLine(aX+28,aY,vX-32,vY);
            // Email envelope in flight
            if(emailProg<1f){
                float ex=aX+(vX-aX)*emailProg;
                float ey=aY+(float)(Math.sin(emailProg*Math.PI)*(-58f));
                // Envelope body
                g2.setColor(new Color(sevB.getRed(),sevB.getGreen(),sevB.getBlue(),210));
                int[] xs={(int)ex-14,(int)ex+14,(int)ex+14,(int)ex-14};
                int[] ys={(int)ey-9,(int)ey-9,(int)ey+9,(int)ey+9};
                g2.fillPolygon(xs,ys,4);
                // Envelope flap
                g2.setColor(new Color(sevA.getRed(),sevA.getGreen(),sevA.getBlue(),200));
                g2.setStroke(S1H);
                g2.drawLine((int)ex-14,(int)ey-9,(int)ex,(int)ey+2);
                g2.drawLine((int)ex+14,(int)ey-9,(int)ex,(int)ey+2);
                g2.setFont(MONO_XS); g2.setColor(sevB);
                g2.drawString("PHISH MAIL",(int)ex-28,(int)ey-18);
            }
            if(clicked){ g2.setFont(new Font("Monospaced",Font.BOLD,12)); g2.setColor(sevA);
                g2.drawString("CREDENTIALS SUBMITTED",vX-155,vY-56); }
            if(currentSeverity>=7&&emailProg>0.7f){
                g2.setFont(MONO_XS); g2.setColor(sevB);
                g2.drawString("EVILGINX: MFA BYPASSED",vX-140,vY-70);
            }
            g2.setFont(MONO_XS); g2.setColor(new Color(180,60,255,165));
            g2.drawString("FROM: cfo@corp-internal-verify.net",aX-28,aY+50);
        }

        // ── Brute Force: password attempt bullets flying to auth portal ────────
        private void drawBruteForce(Graphics2D g2,int w,int h){
            int lX=(int)(w*0.74f),lY=h/2,aX=(int)(w*0.14f),aY=h/2;
            simpleNode(g2,aX,aY,26,sevA,"ATTACKER","");
            shieldNode(g2,lX,lY,32,shield,"AUTH","PORTAL");
            healthBar(g2,lX-50,lY+48,100,8,shield);
            int maxB=Math.min(7+currentSeverity,16);
            for(int i=0;i<Math.min(bfAttempt+1,maxB);i++){
                float t=(progress+i*0.09f)%1.0f;
                float px=aX+(lX-aX)*t,py=aY-36+i*8f;
                float al=t<0.1f?t*10f:t>0.86f?(1f-t)*7f:1f;
                boolean hit=i==bfAttempt&&breached&&progress>0.78f;
                g2.setColor(hit?new Color(0,210,90,(int)(al*210))
                    :new Color(sevA.getRed(),sevA.getGreen(),sevA.getBlue(),(int)(al*210)));
                g2.fillOval((int)px-4,(int)py-4,8,8);
            }
            String[] pwds={"password123","Welcome1!","Summer@24","Qwerty2024",
                "Admin@corp","LetMeIn!","Root2024","CrackMe!!","Hunter2!","Iloveyou1"};
            g2.setFont(MONO_SM); g2.setColor(sevB);
            g2.drawString(String.format("#%03d",bfAttempt+1),aX-14,aY+54);
            g2.setFont(MONO_XS); g2.setColor(new Color(sevB.getRed(),sevB.getGreen(),sevB.getBlue(),170));
            g2.drawString("TRY: "+pwds[bfAttempt%pwds.length],(lX+aX)/2-46,aY-50);
            if(breached&&progress>0.82f){ g2.setFont(new Font("Monospaced",Font.BOLD,13)); g2.setColor(GREEN());
                g2.drawString("MATCH — AUTHENTICATED",lX-138,lY-58); }
            if(currentSeverity>=7){ g2.setFont(MONO_XS); g2.setColor(DIM());
                g2.drawString("[Tor Rotation]",aX-14,aY-28); }
        }

        // ── Zero-Day: exploit probe → shellcode → privilege escalation ─────────
        private void drawZeroDay(Graphics2D g2,int w,int h){
            int sysX=(int)(w*0.60f),sysY=h/2,aX=(int)(w*0.14f),aY=h/2;
            simpleNode(g2,aX,aY,26,sevA,"ATTACKER","");
            shieldNode(g2,sysX,sysY,34,shield,"TARGET","SYSTEM");
            healthBar(g2,sysX-52,sysY+48,104,9,shield);
            // Probe beam
            float beamAlpha=zdProbe;
            g2.setColor(new Color(sevA.getRed(),sevA.getGreen(),sevA.getBlue(),(int)(beamAlpha*180)));
            g2.setStroke(S2);
            g2.drawLine(aX+28,aY,sysX-36,sysY);
            // Stage labels moving along beam
            String[] stageLabels={"RECON","FINGERPRINT","CRAFT EXPLOIT","DELIVER","RCE"};
            for(int s=0;s<=Math.min(zdStage,stageLabels.length-1);s++){
                float sf=(float)s/(stageLabels.length-1);
                int sx=(int)(aX+28+(sysX-36-(aX+28))*sf),sy=sysY+(s%2==0?-24:14);
                g2.setColor(s<=zdStage?sevB:DIM());
                g2.setFont(MONO_XS); g2.drawString(stageLabels[s],sx-24,sy);
                g2.fillOval(sx-4,sysY-4,8,8);
            }
            // Shellcode burst
            if(zdStage>=4&&!breached){
                g2.setFont(MONO_XS); g2.setColor(GREEN());
                g2.drawString("EDR HEURISTIC KILL",sysX-68,sysY-56);
            }
            if(zdStage>=4&&breached){
                g2.setFont(new Font("Monospaced",Font.BOLD,12)); g2.setColor(sevA);
                g2.drawString("RCE ACHIEVED — SHELL ACTIVE",sysX-112,sysY-56);
                if(currentSeverity>=9){ g2.setColor(sevB);
                    g2.drawString("ROOTKIT INSTALLED",sysX-68,sysY-72); }
            }
            // CVE badge
            g2.setFont(MONO_XS); g2.setColor(new Color(sevA.getRed(),sevA.getGreen(),sevA.getBlue(),200));
            g2.drawString("UNPATCHED CVE — NO FIX AVAILABLE",aX-18,aY+56);
        }

        // ── Shared drawing primitives ──────────────────────────────────────────
        private void simpleNode(Graphics2D g2,int cx,int cy,int r,Color col,String l1,String l2){
            g2.setColor(new Color(col.getRed(),col.getGreen(),col.getBlue(),28)); g2.fillOval(cx-r-8,cy-r-8,(r+8)*2,(r+8)*2);
            g2.setColor(new Color(col.getRed(),col.getGreen(),col.getBlue(),60)); g2.fillOval(cx-r,cy-r,r*2,r*2);
            g2.setColor(col); g2.setStroke(S2); g2.drawOval(cx-r,cy-r,r*2,r*2);
            g2.setFont(MONO_XS); g2.setColor(col);
            if(!l1.isEmpty()){ FontMetrics fm=g2.getFontMetrics(); g2.drawString(l1,cx-fm.stringWidth(l1)/2,cy+(l2.isEmpty()?4:-2)); }
            if(!l2.isEmpty()){ FontMetrics fm=g2.getFontMetrics(); g2.drawString(l2,cx-fm.stringWidth(l2)/2,cy+11); }
        }
        private void nodeLabel(Graphics2D g2,int cx,int cy,String l1,String l2,Color col){
            g2.setFont(MONO_XS); g2.setColor(col); FontMetrics fm=g2.getFontMetrics();
            g2.drawString(l1,cx-fm.stringWidth(l1)/2,cy-6); g2.drawString(l2,cx-fm.stringWidth(l2)/2,cy+8);
        }
        private void shieldNode(Graphics2D g2,int cx,int cy,int r,float health,String l1,String l2){
            Color sc=health>0.6f?GREEN():health>0.3f?YELL():RED();
            int rings=Math.min(4+currentSeverity/3,8);
            for(int i=rings-1;i>=0;i--){
                g2.setColor(new Color(sc.getRed(),sc.getGreen(),sc.getBlue(),10));
                int ex=i*10; g2.fillOval(cx-r-ex,cy-r-ex,(r+ex)*2,(r+ex)*2);
            }
            g2.setColor(new Color(sc.getRed(),sc.getGreen(),sc.getBlue(),50)); g2.fillOval(cx-r,cy-r,r*2,r*2);
            g2.setColor(sc); g2.setStroke(S2); g2.drawOval(cx-r,cy-r,r*2,r*2);
            // Hex ring
            Path2D hex=new Path2D.Float(); int hr=r+14;
            double[] ax=new double[6],ay=new double[6];
            for(int i=0;i<6;i++){double a=Math.PI/3*i-Math.PI/6;ax[i]=cx+hr*Math.cos(a);ay[i]=cy+hr*Math.sin(a);}
            hex.moveTo(ax[0],ay[0]);for(int i=1;i<6;i++)hex.lineTo(ax[i],ay[i]);hex.closePath();
            g2.setColor(new Color(sc.getRed(),sc.getGreen(),sc.getBlue(),(int)(health*80)));
            g2.setStroke(S1H); g2.draw(hex);
            g2.setFont(MONO_XS); g2.setColor(sc);
            if(!l1.isEmpty()){ FontMetrics fm=g2.getFontMetrics(); g2.drawString(l1,cx-fm.stringWidth(l1)/2,cy+(l2.isEmpty()?4:-2)); }
            if(!l2.isEmpty()){ FontMetrics fm=g2.getFontMetrics(); g2.drawString(l2,cx-fm.stringWidth(l2)/2,cy+11); }
        }
        private void healthBar(Graphics2D g2,int x,int y,int w,int h,float health){
            g2.setColor(new Color(28,36,52)); g2.fillRoundRect(x,y,w,h,4,4);
            Color bc=health>0.6f?GREEN():health>0.3f?YELL():RED();
            g2.setColor(bc); g2.fillRoundRect(x,y,(int)(w*health),h,4,4);
        }
        private void drawStatusBar(Graphics2D g2,int w,int h){
            String status=active?(tick<28?"[ INITIATING SEQUENCE ]":tick<68?"[ ATTACK IN PROGRESS ]":"[ RESOLVING... ]")
                :(breached?"!! SYSTEM BREACHED !!":">> ATTACK NEUTRALISED <<");
            Color sc=active?sevC:(breached?sevA:GREEN());
            g2.setFont(MONO_SM); g2.setColor(sc);
            FontMetrics fm=g2.getFontMetrics(); g2.drawString(status,(w-fm.stringWidth(status))/2,h-10);
            g2.setFont(MONO_XS); g2.setColor(DIM()); g2.drawString("SHIELD: "+(int)(shield*100)+"%",10,h-22);
            String sevLbl="SEV: "+currentSeverity+"/10 ["+BackendEngine.severityLabel(currentSeverity)+"]";
            g2.setColor(currentSeverity>=8?sevA:currentSeverity>=5?sevC:GREEN());
            g2.drawString(sevLbl,10,h-10);
            // Progress bar
            g2.setColor(new Color(28,36,52)); g2.fillRect(w-114,h-17,104,6);
            g2.setColor(BLUE()); g2.fillRect(w-114,h-17,(int)(104*progress),6);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  LoginPanel
    // ═══════════════════════════════════════════════════════════════════════════
    public static class LoginPanel extends JPanel {
        private final CyberDashboardViewer viewer;
        private JTextField   userField;
        private JPasswordField passField;
        private JLabel       statusLabel;
        private MatrixRain   rain;
        private int          loginAttempts=0;

        LoginPanel(CyberDashboardViewer v){
            viewer=v; setLayout(new GridBagLayout()); setBackground(BG());
            rain=new MatrixRain(this); buildUI();
        }
        @Override protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(BG()); g2.fillRect(0,0,getWidth(),getHeight());
            if(AppCoordinator.ThemeConstants.isDark()) rain.paint(g2,getWidth(),getHeight());
            g2.setColor(new Color(0,0,0,AppCoordinator.ThemeConstants.isDark()?100:40));
            g2.fillRect(0,0,getWidth(),getHeight()); g2.dispose();
        }
        private void buildUI(){
            JPanel box=new JPanel(new GridBagLayout());
            box.setBackground(AppCoordinator.ThemeConstants.isDark()?new Color(16,21,34):new Color(245,248,255));
            box.setOpaque(true);
            box.setBorder(new CompoundBorder(new GlowBorder(BLUE(),2),new EmptyBorder(32,38,28,38)));
            box.setPreferredSize(new Dimension(500,600));
            GridBagConstraints gc=new GridBagConstraints();
            gc.fill=GridBagConstraints.HORIZONTAL; gc.gridx=0; gc.weightx=1.0;

            addRow(box,gc,0,new Insets(0,0,2,0),mkLabel("[ CTTMS ]",new Font("Monospaced",Font.BOLD,36),BLUE(),SwingConstants.CENTER));
            addRow(box,gc,1,new Insets(0,0,1,0),mkLabel("CYBERSECURITY TRAINING & THREAT MONITORING SYSTEM",new Font("Monospaced",Font.BOLD,11),TXT(),SwingConstants.CENTER));
            addRow(box,gc,2,new Insets(0,0,20,0),mkLabel("v"+BackendEngine.VERSION+"  |  Enterprise Edition 2025",new Font("Monospaced",Font.PLAIN,10),DIM(),SwingConstants.CENTER));
            addRow(box,gc,3,new Insets(0,0,14,0),mkLabel("──────────────────────────────────────────",new Font("Monospaced",Font.PLAIN,9),new Color(50,70,110),SwingConstants.CENTER));
            addRow(box,gc,4,new Insets(8,0,3,0),mkLabel("▸  USERNAME",new Font("Monospaced",Font.BOLD,11),BLUE(),SwingConstants.LEFT));
            userField=styledField(BLUE()); userField.setPreferredSize(new Dimension(420,42));
            addRow(box,gc,5,new Insets(0,0,10,0),userField);
            addRow(box,gc,6,new Insets(4,0,3,0),mkLabel("▸  PASSWORD",new Font("Monospaced",Font.BOLD,11),BLUE(),SwingConstants.LEFT));
            passField=styledPass(BLUE()); passField.setPreferredSize(new Dimension(420,42));
            addRow(box,gc,7,new Insets(0,0,20,0),passField);

            JButton loginBtn=new JButton("[ AUTHENTICATE ]"){
                @Override protected void paintComponent(Graphics g){
                    Graphics2D g2=(Graphics2D)g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    Color bg2=getModel().isPressed()?new Color(0,130,65):getModel().isRollover()?new Color(0,200,100):new Color(0,165,80);
                    g2.setColor(bg2); g2.fillRoundRect(0,0,getWidth(),getHeight(),7,7);
                    g2.setColor(Color.BLACK); g2.setFont(getFont()); FontMetrics fm=g2.getFontMetrics();
                    g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);
                    g2.dispose();
                }
            };
            loginBtn.setFont(new Font("Monospaced",Font.BOLD,14));
            loginBtn.setBorderPainted(false); loginBtn.setFocusPainted(false); loginBtn.setContentAreaFilled(false);
            loginBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            loginBtn.setPreferredSize(new Dimension(420,46)); loginBtn.addActionListener(e->doLogin());
            addRow(box,gc,8,new Insets(0,0,8,0),loginBtn);

            statusLabel=new JLabel("  ",SwingConstants.CENTER);
            statusLabel.setFont(new Font("Monospaced",Font.BOLD,12));
            statusLabel.setForeground(RED()); statusLabel.setOpaque(true);
            statusLabel.setBackground(AppCoordinator.ThemeConstants.isDark()?new Color(16,21,34):new Color(245,248,255));
            addRow(box,gc,9,new Insets(0,0,12,0),statusLabel);
            addRow(box,gc,10,new Insets(0,0,10,0),mkLabel("──────────────────────────────────────────",new Font("Monospaced",Font.PLAIN,9),new Color(50,70,110),SwingConstants.CENTER));

            // Demo credentials
            JPanel creds=new JPanel(new GridLayout(4,1,2,3));
            creds.setBackground(AppCoordinator.ThemeConstants.isDark()?new Color(12,16,26):new Color(235,240,252));
            creds.setBorder(new CompoundBorder(new GlowBorder(new Color(40,58,88),1),new EmptyBorder(8,14,8,14)));
            addRow(box,gc,11,new Insets(0,0,2,0),mkLabel("DEMO CREDENTIALS",new Font("Monospaced",Font.BOLD,9),MUTE(),SwingConstants.CENTER));
            String[][] rows={
                {"dr.carter", "Nexus@SecOps#9",   "ADMINISTRATOR"},
                {"r.hayes",   "Hawk3ye$Threat!",   "SR. ANALYST"},
                {"m.okonkwo", "Cipher$0Sigma",     "JR. ANALYST"},
                {"j.novak",   "Tr4inee!Cyber24",   "TRAINEE"}
            };
            Color[] rc={RED(),CYAN(),BLUE(),GREEN()};
            for(int i=0;i<rows.length;i++){
                JLabel cl=new JLabel(String.format("  %-12s / %-18s [%s]",rows[i][0],rows[i][1],rows[i][2]));
                cl.setFont(new Font("Monospaced",Font.PLAIN,10)); cl.setForeground(rc[i]); creds.add(cl);
            }
            addRow(box,gc,12,new Insets(4,0,6,0),creds);
            addRow(box,gc,13,new Insets(10,0,0,0),mkLabel("QuadSquad  |  "+BackendEngine.timestamp().substring(0,10),new Font("Monospaced",Font.PLAIN,9),new Color(45,62,90),SwingConstants.CENTER));

            userField.addActionListener(e->doLogin()); passField.addActionListener(e->doLogin());
            add(box);
        }
        private void addRow(JPanel p,GridBagConstraints gc,int y,Insets ins,Component c){
            gc.gridy=y; gc.insets=ins; p.add(c,gc);
        }
        private JLabel mkLabel(String t,Font f,Color c,int align){
            JLabel l=new JLabel(t,align); l.setFont(f); l.setForeground(c); return l;
        }
        private void doLogin(){
            String u=userField.getText().trim(), p=new String(passField.getPassword());
            if(u.isEmpty()||p.isEmpty()){ statusLabel.setForeground(YELL()); statusLabel.setText("  ⚠  Fields cannot be empty  "); return; }
            User user=viewer.getBackend().getAuthManager().authenticate(u,p);
            if(user!=null){ rain.stop(); statusLabel.setForeground(GREEN()); statusLabel.setText("  ✔  Access granted — loading dashboard...  ");
                Timer t=new Timer(700,e->viewer.onLoginSuccess(user)); t.setRepeats(false); t.start();
            } else { loginAttempts++; statusLabel.setForeground(RED());
                statusLabel.setText("  ✘  Authentication failed (attempt "+loginAttempts+")  ");
                passField.setText(""); viewer.getBackend().getSecurityLog().writeLog("WARNING","Failed login: user="+u);
            }
        }
        void reset(){ userField.setText(""); passField.setText(""); statusLabel.setText("  "); loginAttempts=0; rain.stop(); rain=new MatrixRain(this); }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  DashboardPanel
    // ═══════════════════════════════════════════════════════════════════════════
    public static class DashboardPanel extends JPanel {
        final CyberDashboardViewer viewer;
        JPanel contentArea; CardLayout contentLayout;
        StatCard statSims,statBreaches,statAlerts,statLogs,statUsers;
        SimulationPanel  simPanel;
        LogViewerPanel   logPanel;
        FirewallPanel    fwPanel;
        AnalystPanel     analystPanel;
        ReportPanel      reportPanel;
        AdminPanel       adminPanel;
        TraineeLearnPanel learnPanel;
        AlertsPanel      alertsPanel;

        DashboardPanel(CyberDashboardViewer v){ viewer=v; setLayout(new BorderLayout()); setBackground(BG()); }

        void initForUser(User user){
            removeAll();
            add(buildHeader(user),BorderLayout.NORTH);
            add(buildSidebar(user),BorderLayout.WEST);
            contentLayout=new CardLayout(); contentArea=new JPanel(contentLayout); contentArea.setBackground(BG());
            simPanel    =new SimulationPanel(viewer,this);
            logPanel    =new LogViewerPanel(viewer,this);
            alertsPanel =new AlertsPanel(viewer);
            fwPanel     =new FirewallPanel(viewer);
            analystPanel=new AnalystPanel(viewer,this);
            reportPanel =new ReportPanel(viewer,this);
            adminPanel  =new AdminPanel(viewer,this);
            learnPanel  =new TraineeLearnPanel(viewer);
            contentArea.add(simPanel,"SIM");
            contentArea.add(logPanel,"LOGS");
            contentArea.add(alertsPanel,"ALERTS");
            contentArea.add(fwPanel,"FW");
            contentArea.add(analystPanel,"ANALYST");
            contentArea.add(reportPanel,"REPORT");
            contentArea.add(adminPanel,"ADMIN");
            contentArea.add(learnPanel,"LEARN");
            add(contentArea,BorderLayout.CENTER);
            contentLayout.show(contentArea,"SIM");
            revalidate(); repaint();
        }

        private JPanel buildHeader(User user){
            JPanel outer=new JPanel(new BorderLayout()); outer.setBackground(BG());
            JPanel hdr=new JPanel(new BorderLayout()){
                @Override protected void paintComponent(Graphics g){
                    Graphics2D g2=(Graphics2D)g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setPaint(new GradientPaint(0,0,AppCoordinator.ThemeConstants.bgHeader(),getWidth(),0,
                        AppCoordinator.ThemeConstants.isDark()?new Color(14,20,34):new Color(195,208,230)));
                    g2.fillRect(0,0,getWidth(),getHeight());
                    g2.setColor(new Color(BLUE().getRed(),BLUE().getGreen(),BLUE().getBlue(),42));
                    g2.drawLine(0,getHeight()-1,getWidth(),getHeight()-1); g2.dispose();
                }
            };
            hdr.setOpaque(false); hdr.setPreferredSize(new Dimension(0,52)); hdr.setBorder(new EmptyBorder(0,16,0,16));
            JPanel left=new JPanel(new FlowLayout(FlowLayout.LEFT,10,0)); left.setOpaque(false); left.setBorder(new EmptyBorder(12,0,0,0));
            JLabel logo=new JLabel("⬡ CTTMS"); logo.setFont(new Font("Monospaced",Font.BOLD,18)); logo.setForeground(BLUE());
            JLabel sep=new JLabel("|"); sep.setFont(LABEL_F); sep.setForeground(MUTE());
            JLabel sub=new JLabel("Cybersecurity Training & Threat Monitoring System");
            sub.setFont(new Font("Monospaced",Font.PLAIN,10)); sub.setForeground(DIM());
            left.add(logo); left.add(sep); left.add(sub);

            Color roleCol=user.getRole()==BackendEngine.UserRole.ADMIN?RED():
                user.getRole()==BackendEngine.UserRole.ANALYST?CYAN():GREEN();
            JPanel right=new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0)); right.setOpaque(false); right.setBorder(new EmptyBorder(10,0,0,0));
            // Theme toggle
            JButton themeBtn=cyberBtn(AppCoordinator.ThemeConstants.isDark()?"☀ LIGHT":"🌙 DARK",
                YELL(),AppCoordinator.ThemeConstants.isDark()?new Color(42,36,0):new Color(220,210,170));
            themeBtn.setPreferredSize(new Dimension(100,28));
            themeBtn.addActionListener(e->{ viewer.toggleTheme();
                themeBtn.setText(AppCoordinator.ThemeConstants.isDark()?"☀ LIGHT":"🌙 DARK"); });
            JLabel roleLbl=new JLabel("["+user.getRole()+"]"); roleLbl.setFont(LABEL_F); roleLbl.setForeground(roleCol);
            JLabel userLbl=new JLabel("◉  "+user.getUsername().toUpperCase()); userLbl.setFont(MONO_MD); userLbl.setForeground(TXT());
            JButton logout=cyberBtn("LOGOUT",RED(),new Color(55,18,22)); logout.setPreferredSize(new Dimension(90,28));
            logout.addActionListener(e->viewer.onLogout());
            right.add(themeBtn); right.add(new PulsingDot(roleCol)); right.add(roleLbl); right.add(userLbl); right.add(logout);
            hdr.add(left,BorderLayout.WEST); hdr.add(right,BorderLayout.EAST);

            JPanel statsRow=new JPanel(new GridLayout(1,5,6,0)); statsRow.setBackground(BG()); statsRow.setBorder(new EmptyBorder(5,12,5,12));
            statSims    =new StatCard("SIMULATIONS","0",BLUE());
            statBreaches=new StatCard("BREACHES","0",RED());
            statAlerts  =new StatCard("IDS ALERTS","0",YELL());
            statLogs    =new StatCard("LOG ENTRIES","0",CYAN());
            statUsers   =new StatCard("USERS",String.valueOf(viewer.getBackend().getAuthManager().getUserCount()),GREEN());
            statsRow.add(statSims); statsRow.add(statBreaches); statsRow.add(statAlerts); statsRow.add(statLogs); statsRow.add(statUsers);
            outer.add(hdr,BorderLayout.NORTH); outer.add(statsRow,BorderLayout.SOUTH);
            return outer;
        }

        private JScrollPane buildSidebar(User user){
            JPanel sb=new JPanel(){
                @Override protected void paintComponent(Graphics g){
                    Graphics2D g2=(Graphics2D)g.create();
                    g2.setColor(PANEL()); g2.fillRect(0,0,getWidth(),getHeight());
                    g2.setColor(new Color(BLUE().getRed(),BLUE().getGreen(),BLUE().getBlue(),30));
                    g2.drawLine(getWidth()-1,0,getWidth()-1,getHeight()); g2.dispose();
                }
            };
            sb.setLayout(new BoxLayout(sb,BoxLayout.Y_AXIS)); sb.setBorder(new EmptyBorder(14,0,14,0));
            Color roleCol=user.getRole()==BackendEngine.UserRole.ADMIN?RED():
                user.getRole()==BackendEngine.UserRole.ANALYST?CYAN():GREEN();

            navSection(sb,"GENERAL");
            navBtn(sb,"⚡  Simulation Lab","SIM",GREEN());
            navBtn(sb,"🔔  IDS Alerts","ALERTS",YELL());
            navBtn(sb,"📋  Security Logs","LOGS",CYAN());

            // Role-differentiated navigation
            if(user.getRole()==BackendEngine.UserRole.TRAINEE){
                navSection(sb,"LEARNING"); navBtn(sb,"📖  Learning Module","LEARN",PURP());
                navSection(sb,"MY STATS");
            }
            if(user.getRole()==BackendEngine.UserRole.ANALYST){
                navSection(sb,"ANALYST TOOLS");
                navBtn(sb,"🛡   Firewall View","FW",BLUE());
                navBtn(sb,"🔍  Threat Analysis","ANALYST",CYAN());
                navBtn(sb,"📊  Compliance Report","REPORT",YELL());
            }
            if(user.getRole()==BackendEngine.UserRole.ADMIN){
                navSection(sb,"ANALYST TOOLS");
                navBtn(sb,"🛡   Firewall Config","FW",BLUE());
                navBtn(sb,"🔍  Threat Analysis","ANALYST",CYAN());
                navBtn(sb,"📊  Compliance Report","REPORT",YELL());
                navSection(sb,"ADMINISTRATION");
                navBtn(sb,"⚙   Admin Console","ADMIN",RED());
            }

            sb.add(Box.createVerticalGlue());
            // Permissions box
            JPanel permBox=new JPanel(); permBox.setLayout(new BoxLayout(permBox,BoxLayout.Y_AXIS));
            permBox.setBackground(PANEL());
            permBox.setBorder(new CompoundBorder(new GlowBorder(roleCol,1),new EmptyBorder(8,10,8,10)));
            permBox.setMaximumSize(new Dimension(196,999)); permBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel pt=new JLabel("  PERMISSIONS"); pt.setFont(new Font("Monospaced",Font.BOLD,9)); pt.setForeground(roleCol); permBox.add(pt);
            permBox.add(Box.createVerticalStrut(4));
            for(String perm:user.getPermissions()){
                JLabel pl=new JLabel("  ✓ "+perm); pl.setFont(new Font("Monospaced",Font.PLAIN,9)); pl.setForeground(DIM()); permBox.add(pl);
            }
            JPanel pw=new JPanel(new BorderLayout()); pw.setOpaque(false); pw.setBorder(new EmptyBorder(8,8,8,8));
            pw.add(permBox); pw.setAlignmentX(Component.LEFT_ALIGNMENT); sb.add(pw);

            JScrollPane sc=new JScrollPane(sb); sc.setPreferredSize(new Dimension(204,0));
            sc.setBorder(BorderFactory.createEmptyBorder());
            sc.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            sc.getVerticalScrollBar().setUnitIncrement(12); return sc;
        }
        private void navSection(JPanel p,String t){
            p.add(Box.createVerticalStrut(8));
            JLabel l=new JLabel("  "+t); l.setFont(new Font("Monospaced",Font.BOLD,9)); l.setForeground(DIM());
            l.setAlignmentX(Component.LEFT_ALIGNMENT); p.add(l); p.add(Box.createVerticalStrut(2));
        }
        private void navBtn(JPanel parent,String label,String cardName,Color accent){
            JButton btn=new JButton(label){
                @Override protected void paintComponent(Graphics g){
                    Graphics2D g2=(Graphics2D)g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    if(getModel().isRollover()){
                        g2.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),16)); g2.fillRect(0,0,getWidth(),getHeight());
                        g2.setColor(accent); g2.setStroke(new BasicStroke(2.5f)); g2.drawLine(0,0,0,getHeight());
                    }
                    g2.setColor(getModel().isRollover()?accent:DIM()); g2.setFont(getFont());
                    g2.drawString(getText(),14,getHeight()/2+5); g2.dispose();
                }
            };
            btn.setFont(MONO_SM); btn.setForeground(DIM()); btn.setBorderPainted(false); btn.setFocusPainted(false); btn.setContentAreaFilled(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            Dimension d=new Dimension(204,34); btn.setMaximumSize(d); btn.setPreferredSize(d); btn.setMinimumSize(d);
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);
            btn.addActionListener(e->contentLayout.show(contentArea,cardName)); parent.add(btn);
        }
        void refreshStats(){
            BackendEngine.SimulationController sc=viewer.getBackend().getSimulationController();
            long b=sc.getResultHistory().stream().filter(r->r.breached).count();
            statSims.set(String.valueOf(sc.getResultHistory().size()));
            statBreaches.set(String.valueOf(b));
            statAlerts.set(String.valueOf(sc.getDetector().getAlertCount()));
            statLogs.set(String.valueOf(viewer.getBackend().getSecurityLog().getLogCount()));
            statUsers.set(String.valueOf(viewer.getBackend().getAuthManager().getUserCount()));
            logPanel.refresh(); alertsPanel.refresh();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SimulationPanel  (accessible to all roles, output differs by role)
    // ═══════════════════════════════════════════════════════════════════════════
    public static class SimulationPanel extends JPanel {
        private final CyberDashboardViewer viewer; private final DashboardPanel dash;
        private JComboBox<BackendEngine.AttackType> typeBox;
        private JSlider severitySlider; private JLabel severityLbl,descLbl,resultBadge;
        private JButton launchBtn,clearBtn;
        private AttackAnimPanel animPanel; private TerminalPane terminal;
        private JPanel strongPanel,weakPanel; private JTextArea summaryArea;
        private boolean running=false;

        SimulationPanel(CyberDashboardViewer v,DashboardPanel d){
            viewer=v; dash=d; setBackground(BG()); setLayout(new BorderLayout(8,8)); setBorder(new EmptyBorder(10,12,10,12)); buildUI();
        }
        private void buildUI(){
            JPanel cfg=card("SIMULATION CONFIGURATION",GREEN()); cfg.setLayout(new GridBagLayout());
            GridBagConstraints gc=new GridBagConstraints(); gc.insets=new Insets(4,8,4,8); gc.fill=GridBagConstraints.HORIZONTAL;
            gc.gridx=0;gc.gridy=0;gc.weightx=0;
            JLabel tl=new JLabel("ATTACK VECTOR"); tl.setFont(MONO_XS); tl.setForeground(GREEN()); cfg.add(tl,gc);
            gc.gridx=1;gc.weightx=1;
            typeBox=new JComboBox<>(BackendEngine.AttackType.values());
            typeBox.setBackground(INPUT());typeBox.setForeground(GREEN());typeBox.setFont(MONO_MD);typeBox.setBorder(new GlowBorder(GREEN(),1));
            typeBox.setRenderer((list,val,idx,sel,focus)->{
                JLabel l=new JLabel(val==null?"":val.displayName); l.setFont(MONO_SM);
                l.setForeground(sel?BG():GREEN()); l.setBackground(sel?GREEN():CARD());
                l.setOpaque(true); l.setBorder(new EmptyBorder(3,8,3,8)); return l;
            });
            typeBox.addActionListener(e->updateInfo()); cfg.add(typeBox,gc);
            gc.gridx=2;gc.weightx=0;
            JLabel sl=new JLabel("SEVERITY"); sl.setFont(MONO_XS); sl.setForeground(YELL()); cfg.add(sl,gc);
            gc.gridx=3;gc.weightx=0.35;
            severitySlider=new JSlider(1,10,7); severitySlider.setBackground(CARD()); severitySlider.setForeground(YELL());
            severitySlider.setPaintTicks(true); severitySlider.setMajorTickSpacing(3); severitySlider.setMinorTickSpacing(1);
            severitySlider.addChangeListener(e->severityLbl.setText(severitySlider.getValue()+"/10")); cfg.add(severitySlider,gc);
            gc.gridx=4;gc.weightx=0;
            severityLbl=new JLabel("7/10"); severityLbl.setFont(MONO_LG); severityLbl.setForeground(YELL()); cfg.add(severityLbl,gc);
            gc.gridx=5;
            launchBtn=cyberBtn("▶  LAUNCH",BG(),GREEN()); launchBtn.setPreferredSize(new Dimension(128,34)); launchBtn.addActionListener(e->launchSim()); cfg.add(launchBtn,gc);
            gc.gridx=6;
            clearBtn=cyberBtn("✕  CLEAR",DIM(),CARD()); clearBtn.setPreferredSize(new Dimension(98,34)); clearBtn.addActionListener(e->{terminal.clear();clearResults();}); cfg.add(clearBtn,gc);
            gc.gridx=0;gc.gridy=1;gc.gridwidth=7;gc.weightx=1;
            descLbl=new JLabel(); descLbl.setFont(MONO_XS); descLbl.setForeground(DIM()); descLbl.setBorder(new EmptyBorder(2,8,4,8)); cfg.add(descLbl,gc);
            updateInfo();

            animPanel=new AttackAnimPanel();
            JPanel animCard=card("LIVE ATTACK ANIMATION",BLUE()); animCard.add(animPanel,BorderLayout.CENTER);
            terminal=new TerminalPane();
            JScrollPane ts=new JScrollPane(terminal); ts.setBackground(INPUT()); ts.setBorder(BorderFactory.createEmptyBorder()); styleScroll(ts,GREEN());
            JPanel tc=card("SIMULATION TERMINAL",GREEN()); tc.add(ts,BorderLayout.CENTER);
            JSplitPane cs=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,animCard,tc);
            cs.setBackground(BG()); cs.setBorder(null); cs.setDividerSize(5); cs.setResizeWeight(0.56);

            strongPanel=card("STRONG POINTS",GREEN()); strongPanel.setLayout(new BoxLayout(strongPanel,BoxLayout.Y_AXIS));
            weakPanel=card("WEAK POINTS",RED()); weakPanel.setLayout(new BoxLayout(weakPanel,BoxLayout.Y_AXIS));
            JPanel sw=new JPanel(new GridLayout(1,2,8,0)); sw.setOpaque(false); sw.add(strongPanel); sw.add(weakPanel);
            summaryArea=new JTextArea(3,0); summaryArea.setBackground(INPUT()); summaryArea.setForeground(YELL());
            summaryArea.setFont(MONO_MD); summaryArea.setEditable(false); summaryArea.setLineWrap(true); summaryArea.setWrapStyleWord(true);
            summaryArea.setBorder(new EmptyBorder(8,10,8,10)); summaryArea.setText("▸  Run a simulation to see verdict and analysis.");
            JPanel sc2=card("SIMULATION VERDICT",YELL()); sc2.add(new JScrollPane(summaryArea),BorderLayout.CENTER);
            resultBadge=new JLabel("",SwingConstants.CENTER); resultBadge.setFont(new Font("Monospaced",Font.BOLD,13));
            resultBadge.setOpaque(true); resultBadge.setBackground(CARD()); resultBadge.setBorder(new EmptyBorder(6,16,6,16));
            JPanel bw=new JPanel(new FlowLayout(FlowLayout.RIGHT,0,0)); bw.setOpaque(false); bw.add(resultBadge); sc2.add(bw,BorderLayout.SOUTH);
            JPanel bottom=new JPanel(new BorderLayout(0,6)); bottom.setOpaque(false); bottom.add(sw,BorderLayout.CENTER); bottom.add(sc2,BorderLayout.SOUTH);

            add(cfg,BorderLayout.NORTH); add(cs,BorderLayout.CENTER); add(bottom,BorderLayout.SOUTH);
            terminal.appendLine("[ CTTMS Simulation Engine v"+BackendEngine.VERSION+" ready ]",BLUE());
            terminal.appendLine("[ Select attack vector, set severity (1–10), press LAUNCH ]",DIM());
        }
        private void updateInfo(){
            BackendEngine.AttackType t=(BackendEngine.AttackType)typeBox.getSelectedItem();
            if(t!=null){ descLbl.setText("  ▸  "+t.description); severitySlider.setValue(t.defaultSeverity); severityLbl.setText(t.defaultSeverity+"/10"); }
        }
        private void launchSim(){
            if(running) return; running=true; launchBtn.setEnabled(false); typeBox.setEnabled(false); severitySlider.setEnabled(false);
            BackendEngine.AttackType type=(BackendEngine.AttackType)typeBox.getSelectedItem();
            int sev=severitySlider.getValue(); String sl=BackendEngine.severityLabel(sev);
            clearResults();
            terminal.appendLine("\n"+"═".repeat(56),MUTE());
            terminal.appendLine("["+BackendEngine.timestamp()+"] SIMULATION STARTED",BLUE());
            terminal.appendLine("[ATTACK]    "+type.displayName,ORAN());
            terminal.appendLine("[SEVERITY]  "+sev+"/10  ["+sl+"]",sev>=8?RED():sev>=5?YELL():GREEN());
            terminal.appendLine("[OPERATOR]  "+viewer.getCurrentUser().getUsername().toUpperCase()+" ["+viewer.getCurrentUser().getRole()+"]",DIM());
            terminal.appendLine("─".repeat(56),MUTE());

            SwingWorker<BackendEngine.AttackResult,String[]> worker=new SwingWorker<>(){
                @Override protected BackendEngine.AttackResult doInBackground() throws Exception{
                    BackendEngine.AttackResult r=viewer.getBackend().getSimulationController()
                        .simulate(type,sev,viewer.getCurrentUser().getUsername());
                    for(String ev:r.eventLog){ publish(new String[]{ev}); Thread.sleep(130); }
                    return r;
                }
                @Override protected void process(List<String[]> chunks){
                    for(String[] c:chunks){ String ev=c[0];
                        Color col=ev.contains("BREACH")||ev.contains("CRITICAL")||ev.contains("PWNED")?RED()
                            :ev.contains("BLOCKED")||ev.contains("MITIGATED")||ev.contains("RECOVERY")?GREEN()
                            :ev.contains("FW")||ev.contains("EDR")||ev.contains("WAF")||ev.contains("IDS")||ev.contains("GATEWAY")?BLUE()
                            :ev.contains("EXFIL")||ev.contains("ESC")||ev.contains("LATERAL")||ev.contains("C2")?ORAN()
                            :ev.contains("ENCRYPT")||ev.contains("PROG")||ev.contains("ENC")?PURP()
                            :ev.contains("ALERT")?YELL():TXT();
                        terminal.appendLine(ev,col);
                    }
                }
                @Override protected void done(){
                    try{ BackendEngine.AttackResult r=get();
                        terminal.appendLine("─".repeat(56),MUTE());
                        terminal.appendLine("[RESULT]  "+(r.breached?"!! BREACH CONFIRMED !!":">> ATTACK NEUTRALISED <<"),r.breached?RED():GREEN());
                        terminal.appendLine("[HEALTH]  "+r.finalHealth+"% shield remaining",r.finalHealth>50?GREEN():r.finalHealth>25?YELL():RED());
                        animPanel.start(type,r.breached,sev,()->{
                            showResults(r); running=false; launchBtn.setEnabled(true); typeBox.setEnabled(true); severitySlider.setEnabled(true); dash.refreshStats();
                        });
                    } catch(Exception ex){ running=false; launchBtn.setEnabled(true); typeBox.setEnabled(true); severitySlider.setEnabled(true); }
                }
            };
            worker.execute();
        }
        private void clearResults(){
            SwingUtilities.invokeLater(()->{
                strongPanel.removeAll(); weakPanel.removeAll();
                summaryArea.setText("▸  Simulation in progress..."); resultBadge.setText(""); resultBadge.setBackground(CARD());
                revalidate(); repaint();
            });
        }
        private void showResults(BackendEngine.AttackResult r){
            SwingUtilities.invokeLater(()->{
                strongPanel.removeAll(); JLabel sh=new JLabel("▸  STRONG POINTS"); sh.setFont(LABEL_F); sh.setForeground(GREEN()); sh.setBorder(new EmptyBorder(0,0,6,0)); strongPanel.add(sh);
                for(String s:r.strongPoints){ JLabel l=new JLabel("  ✔  "+s); l.setFont(MONO_SM); l.setForeground(GREEN()); l.setBorder(new EmptyBorder(2,4,2,4)); strongPanel.add(l); }
                strongPanel.add(Box.createVerticalGlue());
                weakPanel.removeAll(); JLabel wh=new JLabel("▸  WEAK POINTS"); wh.setFont(LABEL_F); wh.setForeground(RED()); wh.setBorder(new EmptyBorder(0,0,6,0)); weakPanel.add(wh);
                for(String w:r.weakPoints){ JLabel l=new JLabel("  ✘  "+w); l.setFont(MONO_SM); l.setForeground(ORAN()); l.setBorder(new EmptyBorder(2,4,2,4)); weakPanel.add(l); }
                weakPanel.add(Box.createVerticalGlue());
                summaryArea.setText(r.summary);
                if(r.breached){ resultBadge.setText("  !! BREACH — SEV "+r.severityUsed+"/10 ["+BackendEngine.severityLabel(r.severityUsed)+"]  !!"); resultBadge.setForeground(Color.WHITE); resultBadge.setBackground(new Color(120,20,30)); }
                else { resultBadge.setText("  ✔  NEUTRALISED — SHIELD: "+r.finalHealth+"%  "); resultBadge.setForeground(Color.BLACK); resultBadge.setBackground(new Color(0,155,70)); }
                revalidate(); repaint();
            });
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  TraineeLearnPanel  — rich learning module (Trainee only)
    // ═══════════════════════════════════════════════════════════════════════════
    public static class TraineeLearnPanel extends JPanel {
        private final CyberDashboardViewer viewer;
        TraineeLearnPanel(CyberDashboardViewer v){ viewer=v; setBackground(BG()); setLayout(new BorderLayout(0,8)); setBorder(new EmptyBorder(10,14,10,14)); buildUI(); }
        private void buildUI(){
            JLabel title=new JLabel("▸  CYBERSECURITY LEARNING MODULE"); title.setFont(MONO_LG); title.setForeground(PURP());
            JTabbedPane tabs=new JTabbedPane(); tabs.setBackground(BG()); tabs.setForeground(TXT()); tabs.setFont(LABEL_F);
            // Each tab: one attack topic with detailed write-up
            Object[][] topics={
                {"DDoS",RED(),
                 new String[]{"Overview","Distributed Denial of Service — overwhelms a target with traffic from thousands of compromised hosts (botnets), exhausting bandwidth or CPU so legitimate users cannot connect."},
                 new String[][]{{"How It Works","Bot infection via malware","C2 server assembles zombie army","Attack vectors: UDP flood, SYN flood, HTTP/2 flood","Bandwidth or state exhaustion causes downtime"},
                                {"Severity Scale","LOW (1–3): Single-source flood, easily rate-limited","MEDIUM (4–6): Multi-vector SYN/UDP combo attack","HIGH (7–8): Amplified reflection attack (100Gbps+)","CRITICAL (9–10): Nation-state volumetric, CDN-busting"},
                                {"Defences","Anycast + CDN traffic distribution","BGP blackholing for attacker ASNs","Upstream ISP DDoS scrubbing contract","On-prem volumetric mitigation appliance","Multi-homed ISP links for redundancy"}}},
                {"MITM",CYAN(),
                 new String[]{"Overview","Man-in-the-Middle — attacker positions themselves between two communicating parties, intercepting and potentially altering traffic without either party's knowledge."},
                 new String[][]{{"How It Works","ARP cache poisoning redirects LAN traffic","TLS stripping downgrades HTTPS to HTTP","Rogue TLS cert fools browser (if HSTS absent)","Attacker reads/modifies plaintext stream"},
                                {"Severity Scale","LOW: Passive sniffing only, no active injection","MEDIUM: Credential capture on HTTP endpoints","HIGH: Full session hijack with token cloning","CRITICAL: Persistent MITM with exfiltration"},
                                {"Defences","Dynamic ARP Inspection (DAI) on managed switches","HSTS preloading (max-age ≥ 1 year)","Certificate pinning on mobile clients","802.1X port authentication on wired LAN","mTLS inside service mesh"}}},
                {"SQLi",YELL(),
                 new String[]{"Overview","SQL Injection — malicious SQL fragments injected through unsanitised input fields to manipulate the database query logic, enabling data theft, authentication bypass, or data destruction."},
                 new String[][]{{"How It Works","Tautology: ' OR '1'='1 — bypasses login","UNION SELECT — extracts data from other tables","Blind/Time-delay: SLEEP(5) — confirms injection point","Error-based — leaks DB version from error messages"},
                                {"Severity Scale","LOW: Read-only data probe, no exfil","MEDIUM: Auth bypass, limited record dump","HIGH: Full table dump, hash cracking","CRITICAL: Schema mapping, DROP TABLE, RCE via xp_cmdshell"},
                                {"Defences","Parameterised queries / prepared statements everywhere","WAF with OWASP Core Rule Set v3.3","Least-privilege DB accounts (SELECT only for API)","Sanitise error messages — never return stack traces","Input length + type validation on every endpoint"}}},
                {"Ransomware",ORAN(),
                 new String[]{"Overview","Ransomware — malware that encrypts victim files and demands cryptocurrency payment for the decryption key, often spreading laterally across a network before detonating."},
                 new String[][]{{"Kill Chain","Delivery: phishing email with malicious macro","Execution: macro drops encrypted payload","Persistence: registry Run key","Recon: enumerate file extensions to target","Encryption: AES-256-CBC on each file","C2: beacon exfiltrates key + demands ransom"},
                                {"Severity Scale","LOW: Single endpoint, early EDR kill","MEDIUM: Partial encryption before detection","HIGH: Full endpoint + shadow copy deletion","CRITICAL: Lateral spread to file shares + DC"},
                                {"Defences","EDR with behavioural heuristics (mass-rename detection)","Immutable air-gapped backups (test restores monthly!)","Email attachment sandboxing / detonation","Block Office macros via Group Policy","SMB signing — blocks unauthenticated lateral movement","Egress filtering — blocks C2 beacons"}}},
                {"Phishing",PURP(),
                 new String[]{"Overview","Phishing / Spear-Phishing — deceptive emails craft convincing lures to trick users into submitting credentials to attacker-controlled fake portals, often combined with real-time MFA bypass proxies."},
                 new String[][]{{"How It Works","OSINT: LinkedIn/company site to personalise lure","Lookalike domain (<24h old, evades blacklists)","Spoofed DKIM via lookalike domain SMTP","Evilginx2 real-time proxy — relays and captures OTP","BEC follow-up: forged internal wire transfer request"},
                                {"Severity Scale","LOW: Generic mass phish, poor lure quality","MEDIUM: Spear-phish with personalised content","HIGH: Real-time MFA bypass (Evilginx)","CRITICAL: BEC with fraudulent payment initiation"},
                                {"Defences","Time-of-click URL sandbox in email gateway","DMARC + DKIM + SPF on all owned domains","FIDO2/WebAuthn hardware tokens (phishing-resistant MFA)","New domain registration monitoring (<24h alerts)","Quarterly phishing simulation exercises","External email warning banners"}}},
                {"Brute Force",GREEN(),
                 new String[]{"Overview","Brute Force / Credential Stuffing — automated tools systematically try passwords from dictionaries or leaked credential dumps, often distributed across many IPs to evade per-IP rate limiting."},
                 new String[][]{{"Attack Types","Dictionary: rockyou.txt / company mutations","Credential stuffing: reuse of HaveIBeenPwned pairs","Password spraying: 1 common password × many accounts","Distributed slow-and-low: evades burst detection"},
                                {"Severity Scale","LOW: Single-source burst, blocked by lockout","MEDIUM: Distributed from multiple IPs","HIGH: Tor-rotated slow-and-low, hard to detect","CRITICAL: Compromised account used as launchpad"},
                                {"Defences","Account lockout (per-account, not just per-IP)","CAPTCHA on all public-facing login endpoints","HIBP API check on login (breach password detection)","MFA — renders password-only attacks useless","Anomaly detection for distributed low-volume attempts"}}},
                {"Zero-Day",new Color(180,0,255),
                 new String[]{"Overview","Zero-Day Exploit — attacks a vulnerability in software that has not yet been publicly disclosed or patched. Often used by nation-state APTs for stealthy access. Detection relies entirely on behaviour, not signatures."},
                 new String[][]{{"How It Works","Vulnerability research or purchase on dark web","Custom shellcode crafted for target architecture","Delivery via spear-phish or supply-chain compromise","Privilege escalation to SYSTEM/root","Rootkit installation for persistence across reboots"},
                                {"Severity Scale","LOW: Limited scope, no privilege escalation","MEDIUM: Local privilege escalation only","HIGH: Remote code execution (RCE), data exfil","CRITICAL: RCE + lateral movement + rootkit + APT implant"},
                                {"Defences","Patch management — shrink attack surface","EDR heuristic engine (behaviour, not signatures)","WAF virtual patching as interim measure","Threat intelligence feed (known APT IOCs)","SBOM — know every dependency and its CVE status","Vulnerability disclosure programme (VDP)"}}},
            };
            for(Object[] topic:(Object[][])topics){
                String name=(String)topic[0]; Color col=(Color)topic[1];
                String[] overview=(String[])topic[2]; Object[][] sections=(Object[][])topic[3];
                JPanel topicPanel=new JPanel(); topicPanel.setLayout(new BoxLayout(topicPanel,BoxLayout.Y_AXIS));
                topicPanel.setBackground(BG()); topicPanel.setBorder(new EmptyBorder(14,18,14,18));
                // Overview
                JLabel ovHead=new JLabel(overview[0]); ovHead.setFont(new Font("SansSerif",Font.BOLD,14)); ovHead.setForeground(col); ovHead.setAlignmentX(Component.LEFT_ALIGNMENT); topicPanel.add(ovHead);
                topicPanel.add(Box.createVerticalStrut(6));
                JTextArea ovText=new JTextArea(overview[1]); ovText.setFont(MONO_SM); ovText.setForeground(TXT()); ovText.setBackground(BG()); ovText.setEditable(false); ovText.setLineWrap(true); ovText.setWrapStyleWord(true); ovText.setAlignmentX(Component.LEFT_ALIGNMENT); topicPanel.add(ovText);
                topicPanel.add(Box.createVerticalStrut(14));
                for(Object[] sec:(Object[][])sections){
                    String[] lines=(String[])sec;
                    JLabel secHead=new JLabel("► "+lines[0]); secHead.setFont(new Font("Monospaced",Font.BOLD,12)); secHead.setForeground(col); secHead.setAlignmentX(Component.LEFT_ALIGNMENT); topicPanel.add(secHead);
                    topicPanel.add(Box.createVerticalStrut(4));
                    for(int i=1;i<lines.length;i++){
                        JLabel bl=new JLabel("    •  "+lines[i]); bl.setFont(MONO_SM); bl.setForeground(TXT()); bl.setAlignmentX(Component.LEFT_ALIGNMENT); topicPanel.add(bl); topicPanel.add(Box.createVerticalStrut(2));
                    }
                    topicPanel.add(Box.createVerticalStrut(10));
                }
                JScrollPane sp=new JScrollPane(topicPanel); sp.setBorder(BorderFactory.createEmptyBorder()); styleScroll(sp,col);
                tabs.addTab(name,sp); tabs.setForegroundAt(tabs.getTabCount()-1,col);
            }
            // Trainee stats bar
            Trainee t=(viewer.getCurrentUser() instanceof Trainee)?(Trainee)viewer.getCurrentUser():null;
            JPanel statsPanel=new JPanel(new GridLayout(1,3,8,0)); statsPanel.setOpaque(false);
            statsPanel.add(new StatCard("YOUR SIMULATIONS",t!=null?String.valueOf(t.getSimulationsRun()):"0",BLUE()));
            statsPanel.add(new StatCard("QUIZ SCORE",t!=null?t.getQuizScore()+"%":"—",YELL()));
            statsPanel.add(new StatCard("RANK","JUNIOR ANALYST",GREEN()));
            add(title,BorderLayout.NORTH); add(tabs,BorderLayout.CENTER); add(statsPanel,BorderLayout.SOUTH);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  LogViewerPanel
    // ═══════════════════════════════════════════════════════════════════════════
    public static class LogViewerPanel extends JPanel {
        private final CyberDashboardViewer viewer; private final DashboardPanel dash;
        private JTextArea logArea; private JLabel countLbl; private JTextField filterField;
        LogViewerPanel(CyberDashboardViewer v,DashboardPanel d){ viewer=v;dash=d;setBackground(BG());setLayout(new BorderLayout(0,8));setBorder(new EmptyBorder(10,12,10,12));buildUI(); }
        private void buildUI(){
            JLabel title=new JLabel("▸  SECURITY AUDIT LOG"); title.setFont(MONO_LG); title.setForeground(CYAN());
            filterField=styledField(CYAN()); filterField.setPreferredSize(new Dimension(180,28)); filterField.setToolTipText("Filter keyword"); filterField.addActionListener(e->refresh());
            JButton rb=cyberBtn("⟳ REFRESH",CYAN(),new Color(0,36,46)); rb.setPreferredSize(new Dimension(106,28)); rb.addActionListener(e->refresh());
            JButton fb=cyberBtn("FILTER",CYAN(),new Color(0,36,46)); fb.setPreferredSize(new Dimension(80,28)); fb.addActionListener(e->refresh());
            countLbl=new JLabel("0 entries"); countLbl.setFont(MONO_XS); countLbl.setForeground(DIM());
            JPanel ctrl=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); ctrl.setOpaque(false);
            JLabel fl=new JLabel("Filter:"); fl.setFont(MONO_XS); fl.setForeground(DIM());
            ctrl.add(fl); ctrl.add(filterField); ctrl.add(fb); ctrl.add(rb); ctrl.add(countLbl);
            JPanel top=new JPanel(new BorderLayout()); top.setOpaque(false); top.add(title,BorderLayout.WEST); top.add(ctrl,BorderLayout.EAST);
            logArea=new JTextArea(); logArea.setBackground(INPUT()); logArea.setForeground(CYAN()); logArea.setFont(MONO_SM); logArea.setEditable(false); logArea.setLineWrap(false);
            JScrollPane sc=new JScrollPane(logArea); sc.setBorder(new GlowBorder(CYAN(),1)); styleScroll(sc,CYAN());
            JPanel legend=new JPanel(new FlowLayout(FlowLayout.LEFT,16,4)); legend.setBackground(CARD()); legend.setBorder(new CompoundBorder(new GlowBorder(MUTE(),1),new EmptyBorder(4,10,4,10)));
            for(Object[] e:new Object[][]{{"[CRITICAL]",RED()},{"[WARNING]",YELL()},{"[INFO]",CYAN()}}){
                JLabel l=new JLabel((String)e[0]); l.setFont(MONO_XS); l.setForeground((Color)e[1]); legend.add(l); }
            add(top,BorderLayout.NORTH); add(sc,BorderLayout.CENTER); add(legend,BorderLayout.SOUTH);
        }
        void refresh(){
            List<String> logs=viewer.getBackend().getSecurityLog().readLogs();
            String f=filterField!=null?filterField.getText().trim().toLowerCase():"";
            StringBuilder sb=new StringBuilder(); int n=0;
            for(String log:logs) if(f.isEmpty()||log.toLowerCase().contains(f)){ sb.append(log).append("\n"); n++; }
            logArea.setText(sb.toString()); logArea.setCaretPosition(logArea.getDocument().getLength());
            countLbl.setText(n+" entries");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  AlertsPanel
    // ═══════════════════════════════════════════════════════════════════════════
    public static class AlertsPanel extends JPanel {
        private final CyberDashboardViewer viewer; private TerminalPane alertPane;
        AlertsPanel(CyberDashboardViewer v){ viewer=v;setBackground(BG());setLayout(new BorderLayout(0,8));setBorder(new EmptyBorder(10,12,10,12));buildUI(); }
        private void buildUI(){
            JLabel title=new JLabel("▸  IDS / THREAT DETECTOR LIVE ALERTS"); title.setFont(MONO_LG); title.setForeground(YELL());
            JButton clr=cyberBtn("CLEAR",YELL(),new Color(44,36,0)); clr.setPreferredSize(new Dimension(88,28)); clr.addActionListener(e->{ alertPane.clear(); alertPane.appendLine("[ Feed cleared ]",DIM()); });
            JPanel top=new JPanel(new BorderLayout()); top.setOpaque(false); top.add(title,BorderLayout.WEST); top.add(clr,BorderLayout.EAST);
            alertPane=new TerminalPane(); alertPane.setForeground(YELL());
            JScrollPane sc=new JScrollPane(alertPane); sc.setBorder(new GlowBorder(YELL(),1)); styleScroll(sc,YELL());
            ThreatDetector det=viewer.getBackend().getSimulationController().getDetector();
            JPanel stats=new JPanel(new GridLayout(1,3,8,0)); stats.setOpaque(false);
            JPanel m=card("DETECTION MODE",BLUE()); JLabel ml=new JLabel(det.getDetectionMode()); ml.setFont(MONO_LG); ml.setForeground(BLUE()); m.add(ml,BorderLayout.CENTER);
            JPanel s=card("IDS STATUS",det.isActive()?GREEN():RED()); JLabel sl=new JLabel(det.isActive()?"ACTIVE":"DISABLED"); sl.setFont(MONO_LG); sl.setForeground(det.isActive()?GREEN():RED()); s.add(sl,BorderLayout.CENTER);
            JPanel c=card("TOTAL ALERTS",ORAN()); JLabel cl=new JLabel(String.valueOf(det.getAlertCount())); cl.setFont(MONO_LG); cl.setForeground(ORAN()); c.add(cl,BorderLayout.CENTER);
            stats.add(m); stats.add(s); stats.add(c);
            add(top,BorderLayout.NORTH); add(sc,BorderLayout.CENTER); add(stats,BorderLayout.SOUTH); refresh();
        }
        void refresh(){ alertPane.clear(); List<String> al=viewer.getBackend().getSimulationController().getDetector().getAlertHistory();
            if(al.isEmpty()){ alertPane.appendLine("[ No IDS alerts this session ]",DIM()); return; }
            for(String a:al) alertPane.appendLine(a,YELL());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  FirewallPanel  (Analyst: read-only view | Admin: full config)
    // ═══════════════════════════════════════════════════════════════════════════
    public static class FirewallPanel extends JPanel {
        private final CyberDashboardViewer viewer;
        private JTextArea rulesArea; private JTextField newRuleField;
        private JSlider threshSlider; private JLabel threshLbl,statsLbl;
        FirewallPanel(CyberDashboardViewer v){ viewer=v;setBackground(BG());setLayout(new BorderLayout(0,8));setBorder(new EmptyBorder(10,12,10,12));buildUI(); }
        private void buildUI(){
            boolean isAdmin=viewer.getCurrentUser()!=null&&viewer.getCurrentUser().getRole()==BackendEngine.UserRole.ADMIN;
            Firewall fw=viewer.getBackend().getSimulationController().getFirewall();
            JLabel title=new JLabel("▸  FIREWALL "+(isAdmin?"CONFIGURATION":"INSPECTOR (READ-ONLY)")); title.setFont(MONO_LG); title.setForeground(BLUE());
            JPanel ctrl=new JPanel(new FlowLayout(FlowLayout.LEFT,14,0)); ctrl.setBackground(CARD()); ctrl.setBorder(new CompoundBorder(new GlowBorder(BLUE(),1),new EmptyBorder(8,10,8,10)));
            JLabel tl=new JLabel("Severity Threshold:"); tl.setFont(LABEL_F); tl.setForeground(DIM());
            threshSlider=new JSlider(1,10,fw.getThresholdSeverity()); threshSlider.setBackground(CARD()); threshSlider.setForeground(BLUE()); threshSlider.setPaintTicks(true); threshSlider.setMajorTickSpacing(3); threshSlider.setEnabled(isAdmin);
            threshLbl=new JLabel(String.valueOf(fw.getThresholdSeverity())); threshLbl.setFont(MONO_LG); threshLbl.setForeground(BLUE());
            threshSlider.addChangeListener(e->{ if(isAdmin){ fw.setThresholdSeverity(threshSlider.getValue()); threshLbl.setText(String.valueOf(threshSlider.getValue())); refreshRules(); }});
            JLabel enl=new JLabel("  Enabled:"); enl.setFont(LABEL_F); enl.setForeground(DIM());
            JCheckBox enBox=new JCheckBox("",fw.isEnabled()); enBox.setBackground(CARD()); enBox.setEnabled(isAdmin);
            enBox.addActionListener(e->{ if(isAdmin){ fw.setEnabled(enBox.isSelected()); refreshRules(); }});
            statsLbl=new JLabel(); statsLbl.setFont(MONO_XS); statsLbl.setForeground(DIM());
            ctrl.add(tl); ctrl.add(threshSlider); ctrl.add(threshLbl); ctrl.add(enl); ctrl.add(enBox); ctrl.add(statsLbl);
            JPanel top=new JPanel(new BorderLayout(0,6)); top.setOpaque(false); top.add(title,BorderLayout.NORTH); top.add(ctrl,BorderLayout.SOUTH);
            rulesArea=new JTextArea(); rulesArea.setBackground(INPUT()); rulesArea.setForeground(BLUE()); rulesArea.setFont(MONO_MD); rulesArea.setEditable(false);
            JScrollPane sc=new JScrollPane(rulesArea); sc.setBorder(new GlowBorder(BLUE(),1)); styleScroll(sc,BLUE()); refreshRules();
            add(top,BorderLayout.NORTH); add(sc,BorderLayout.CENTER);
            if(isAdmin){
                JPanel addRow=new JPanel(new BorderLayout(8,0)); addRow.setBackground(CARD());
                addRow.setBorder(new CompoundBorder(new GlowBorder(BLUE(),1),new EmptyBorder(8,10,8,10)));
                JLabel addLbl=new JLabel("New Rule (BLOCK:SIG:severity>N):"); addLbl.setFont(LABEL_F); addLbl.setForeground(DIM());
                newRuleField=styledField(BLUE()); JButton addBtn=cyberBtn("ADD RULE",BLUE(),new Color(0,26,54));
                addBtn.addActionListener(e->{ String rule=newRuleField.getText().trim(); if(!rule.isEmpty()){ fw.addRule(rule); viewer.getBackend().getSecurityLog().writeLog("INFO","FW rule added: "+rule); newRuleField.setText(""); refreshRules(); }});
                newRuleField.addActionListener(e->addBtn.doClick());
                JPanel ar=new JPanel(new BorderLayout(6,0)); ar.setOpaque(false); ar.add(newRuleField,BorderLayout.CENTER); ar.add(addBtn,BorderLayout.EAST);
                addRow.add(addLbl,BorderLayout.WEST); addRow.add(ar,BorderLayout.CENTER);
                add(addRow,BorderLayout.SOUTH);
            }
        }
        private void refreshRules(){
            Firewall fw=viewer.getBackend().getSimulationController().getFirewall();
            statsLbl.setText("  Inspected: "+fw.getPacketsInspected()+"  Blocked: "+fw.getPacketsBlocked());
            StringBuilder sb=new StringBuilder();
            sb.append("# FIREWALL  Threshold=").append(fw.getThresholdSeverity()).append("  Status=").append(fw.isEnabled()?"ENABLED":"DISABLED").append("\n\n");
            List<String> rules=fw.getRules();
            for(int i=0;i<rules.size();i++) sb.append(String.format("  [%02d]  %s\n",i+1,rules.get(i)));
            sb.append("\n# HIT STATISTICS:\n");
            fw.getHitCounts().forEach((k,v)->sb.append(String.format("  %-42s %d hits\n",k,v)));
            rulesArea.setText(sb.toString());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  AnalystPanel  (Analyst + Admin)
    // ═══════════════════════════════════════════════════════════════════════════
    public static class AnalystPanel extends JPanel {
        private final CyberDashboardViewer viewer; private final DashboardPanel dash;
        private DefaultTableModel tableModel; private JTable table; private JTextArea detailArea;
        AnalystPanel(CyberDashboardViewer v,DashboardPanel d){ viewer=v;dash=d;setBackground(BG());setLayout(new BorderLayout(0,8));setBorder(new EmptyBorder(10,12,10,12));buildUI(); }
        private void buildUI(){
            JLabel title=new JLabel("▸  THREAT ANALYSIS — SIMULATION HISTORY"); title.setFont(MONO_LG); title.setForeground(CYAN());
            JButton rb=cyberBtn("⟳ REFRESH",CYAN(),new Color(0,36,46)); rb.setPreferredSize(new Dimension(108,28)); rb.addActionListener(e->refresh());
            JPanel top=new JPanel(new BorderLayout()); top.setOpaque(false); top.add(title,BorderLayout.WEST); top.add(rb,BorderLayout.EAST);
            String[] cols={"#","TIMESTAMP","ATTACK TYPE","SEVERITY","RESULT","SHIELD %","INITIATOR"};
            tableModel=new DefaultTableModel(cols,0){ @Override public boolean isCellEditable(int r,int c){ return false; }};
            table=new JTable(tableModel); table.setBackground(CARD()); table.setForeground(TXT()); table.setFont(MONO_SM);
            table.setRowHeight(22); table.setGridColor(new Color(26,34,52)); table.setSelectionBackground(new Color(0,75,135));
            table.getTableHeader().setBackground(PANEL()); table.getTableHeader().setForeground(CYAN()); table.getTableHeader().setFont(LABEL_F);
            table.setDefaultRenderer(Object.class,(t,val,sel,foc,row,col)->{
                JLabel lbl=new JLabel(val==null?"":val.toString()); lbl.setFont(MONO_SM);
                lbl.setBackground(sel?new Color(0,60,110):row%2==0?CARD():new Color(17,23,36));
                lbl.setOpaque(true); lbl.setBorder(new EmptyBorder(2,6,2,6));
                if(col==4) lbl.setForeground(val!=null&&val.toString().contains("BREACH")?RED():GREEN());
                else lbl.setForeground(sel?BRT():TXT()); return lbl;
            });
            table.getSelectionModel().addListSelectionListener(e->showDetail());
            JScrollPane ts=new JScrollPane(table); ts.setBorder(new GlowBorder(CYAN(),1)); styleScroll(ts,CYAN());
            detailArea=new JTextArea(8,0); detailArea.setBackground(INPUT()); detailArea.setForeground(CYAN()); detailArea.setFont(MONO_SM); detailArea.setEditable(false); detailArea.setLineWrap(true); detailArea.setWrapStyleWord(true);
            detailArea.setText("[ Select a simulation run above to view full event log ]");
            JScrollPane ds=new JScrollPane(detailArea); ds.setBorder(new GlowBorder(CYAN(),1)); styleScroll(ds,CYAN());
            JPanel dc=card("SELECTED RUN — FULL EVENT LOG",CYAN()); dc.add(ds,BorderLayout.CENTER);
            JSplitPane sp=new JSplitPane(JSplitPane.VERTICAL_SPLIT,ts,dc); sp.setBorder(null); sp.setDividerSize(5); sp.setResizeWeight(0.55);
            add(top,BorderLayout.NORTH); add(sp,BorderLayout.CENTER); refresh();
        }
        void refresh(){
            tableModel.setRowCount(0); List<BackendEngine.AttackResult> h=viewer.getBackend().getSimulationController().getResultHistory();
            for(int i=0;i<h.size();i++){ BackendEngine.AttackResult r=h.get(i);
                tableModel.addRow(new Object[]{i+1,r.timestamp,r.attackType,
                    r.severityUsed+"/10 ["+BackendEngine.severityLabel(r.severityUsed)+"]",
                    r.breached?"!! BREACH":"✔ BLOCKED",r.finalHealth+"%",r.initiator}); }
        }
        private void showDetail(){
            int row=table.getSelectedRow(); List<BackendEngine.AttackResult> h=viewer.getBackend().getSimulationController().getResultHistory();
            if(row<0||row>=h.size()) return; BackendEngine.AttackResult r=h.get(row);
            StringBuilder sb=new StringBuilder();
            sb.append("=== RUN #").append(row+1).append(" — ").append(r.attackType).append(" ===\n");
            sb.append("Result: ").append(r.breached?"BREACH":"NEUTRALISED").append("  |  Shield: ").append(r.finalHealth).append("%  |  Severity: ").append(r.severityUsed).append("/10 [").append(BackendEngine.severityLabel(r.severityUsed)).append("]\n\n");
            sb.append("--- EVENT LOG ---\n"); for(String ev:r.eventLog) sb.append(ev).append("\n");
            sb.append("\n--- VERDICT ---\n").append(r.summary).append("\n\n");
            sb.append("--- STRONG POINTS ---\n"); for(String s:r.strongPoints) sb.append("  ✔ ").append(s).append("\n");
            sb.append("\n--- WEAK POINTS ---\n"); for(String w:r.weakPoints) sb.append("  ✘ ").append(w).append("\n");
            detailArea.setText(sb.toString()); detailArea.setCaretPosition(0);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  ReportPanel  (Analyst + Admin)
    // ═══════════════════════════════════════════════════════════════════════════
    public static class ReportPanel extends JPanel {
        private final CyberDashboardViewer viewer; private final DashboardPanel dash;
        private JTextArea reportArea; private StatCard sc1,sc2,sc3,sc4;
        ReportPanel(CyberDashboardViewer v,DashboardPanel d){ viewer=v;dash=d;setBackground(BG());setLayout(new BorderLayout(0,8));setBorder(new EmptyBorder(10,12,10,12));buildUI(); }
        private void buildUI(){
            JLabel title=new JLabel("▸  SECURITY COMPLIANCE REPORT GENERATOR"); title.setFont(MONO_LG); title.setForeground(YELL());
            JButton gen=cyberBtn("⚙  GENERATE",YELL(),new Color(42,34,0)); gen.setPreferredSize(new Dimension(136,30)); gen.addActionListener(e->generate());
            JButton exp=cyberBtn("📁  EXPORT",CYAN(),new Color(0,30,42)); exp.setPreferredSize(new Dimension(114,30)); exp.addActionListener(e->exportReport());
            JPanel btns=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); btns.setOpaque(false); btns.add(gen); btns.add(exp);
            JPanel top=new JPanel(new BorderLayout()); top.setOpaque(false); top.add(title,BorderLayout.WEST); top.add(btns,BorderLayout.EAST);
            reportArea=new JTextArea(); reportArea.setBackground(INPUT()); reportArea.setForeground(YELL()); reportArea.setFont(MONO_SM); reportArea.setEditable(false);
            reportArea.setText("[ Click GENERATE to compile the security compliance report ]");
            JScrollPane sc=new JScrollPane(reportArea); sc.setBorder(new GlowBorder(YELL(),1)); styleScroll(sc,YELL());
            JPanel sr=new JPanel(new GridLayout(1,4,8,0)); sr.setOpaque(false); sr.setBorder(new EmptyBorder(6,0,0,0));
            sc1=new StatCard("SIMULATIONS","0",BLUE()); sc2=new StatCard("BREACHES","0",RED()); sc3=new StatCard("NEUTRALISED","0",GREEN()); sc4=new StatCard("BREACH RATE","0%",ORAN());
            sr.add(sc1); sr.add(sc2); sr.add(sc3); sr.add(sc4);
            add(top,BorderLayout.NORTH); add(sc,BorderLayout.CENTER); add(sr,BorderLayout.SOUTH);
        }
        private void generate(){
            User u=viewer.getCurrentUser();
            List<BackendEngine.AttackResult> history=viewer.getBackend().getSimulationController().getResultHistory();
            String report;
            if(u instanceof SecurityAnalyst) report=((SecurityAnalyst)u).generateSecurityReport(viewer.getBackend().getSecurityLog(),history);
            else{ SecurityAnalyst proxy=new SecurityAnalyst("PX","admin_proxy","","","ADMIN-LEVEL"); report=proxy.generateSecurityReport(viewer.getBackend().getSecurityLog(),history); }
            reportArea.setText(report); reportArea.setCaretPosition(0);
            long b=history.stream().filter(r->r.breached).count(); long bl=history.size()-b;
            sc1.set(String.valueOf(history.size())); sc2.set(String.valueOf(b)); sc3.set(String.valueOf(bl));
            sc4.set(history.size()>0?String.format("%.0f%%",b*100.0/history.size()):"0%");
        }
        private void exportReport(){
            String content=reportArea.getText(); if(content.startsWith("[")){ JOptionPane.showMessageDialog(this,"Generate a report first.","Export",JOptionPane.WARNING_MESSAGE); return; }
            try{ java.io.File f=new java.io.File("logs/compliance_report_"+System.currentTimeMillis()+".txt"); f.getParentFile().mkdirs();
                try(java.io.FileWriter fw=new java.io.FileWriter(f)){ fw.write(content); }
                JOptionPane.showMessageDialog(this,"Saved:\n"+f.getAbsolutePath(),"Export OK",JOptionPane.INFORMATION_MESSAGE);
            } catch(Exception ex){ JOptionPane.showMessageDialog(this,"Export failed: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE); }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  AdminPanel  (Admin only)
    // ═══════════════════════════════════════════════════════════════════════════
    public static class AdminPanel extends JPanel {
        private final CyberDashboardViewer viewer; private final DashboardPanel dash;
        private JTextArea outputArea; private DefaultTableModel userModel; private JTable userTable;
        AdminPanel(CyberDashboardViewer v,DashboardPanel d){ viewer=v;dash=d;setBackground(BG());setLayout(new BorderLayout(0,8));setBorder(new EmptyBorder(10,12,10,12));buildUI(); }
        private void buildUI(){
            JLabel title=new JLabel("▸  ADMINISTRATOR CONTROL PANEL"); title.setFont(MONO_LG); title.setForeground(RED());
            String[] cols={"ID","USERNAME","ROLE","EMAIL","LOGINS","ACTIVE"};
            userModel=new DefaultTableModel(cols,0){ @Override public boolean isCellEditable(int r,int c){ return false; }};
            userTable=new JTable(userModel); userTable.setBackground(CARD()); userTable.setForeground(TXT()); userTable.setFont(MONO_SM);
            userTable.setRowHeight(22); userTable.setGridColor(new Color(26,34,52)); userTable.setSelectionBackground(new Color(75,18,24));
            userTable.getTableHeader().setBackground(PANEL()); userTable.getTableHeader().setForeground(RED()); userTable.getTableHeader().setFont(LABEL_F);
            userTable.setDefaultRenderer(Object.class,(t,val,sel,foc,row,col)->{
                JLabel lbl=new JLabel(val==null?"":val.toString()); lbl.setFont(MONO_SM);
                lbl.setBackground(sel?new Color(75,18,24):row%2==0?CARD():new Color(17,23,36));
                lbl.setOpaque(true); lbl.setBorder(new EmptyBorder(2,6,2,6));
                if(col==2){ String role=val==null?"":val.toString(); lbl.setForeground(role.contains("ADMIN")?RED():role.contains("ANALYST")?CYAN():GREEN()); }
                else lbl.setForeground(sel?BRT():TXT()); return lbl;
            });
            refreshTable();
            JScrollPane us=new JScrollPane(userTable); us.setBorder(new GlowBorder(RED(),1)); styleScroll(us,RED());
            JPanel uc=card("USER REGISTRY — ACCOUNT MANAGEMENT",RED()); uc.add(us,BorderLayout.CENTER);
            JPanel btnRow=new JPanel(new FlowLayout(FlowLayout.LEFT,8,6)); btnRow.setBackground(CARD()); btnRow.setBorder(new CompoundBorder(new GlowBorder(RED(),1),new EmptyBorder(6,8,6,8)));
            JButton addU=cyberBtn("➕  ADD USER",GREEN(),new Color(0,34,16));
            JButton delU=cyberBtn("🗑  DELETE",RED(),new Color(50,14,18));
            JButton chgP=cyberBtn("🔑  CHG PW",BLUE(),new Color(0,24,50));
            JButton clrL=cyberBtn("✕  CLEAR LOGS",YELL(),new Color(40,32,0));
            JButton rstI=cyberBtn("⟳  RESET IDS",CYAN(),new Color(0,30,38));
            JButton sysI=cyberBtn("ℹ  SYS INFO",GREEN(),new Color(0,28,14));
            JButton tglF=cyberBtn("🛡  TOGGLE FW",BLUE(),new Color(0,20,46));
            JButton audt=cyberBtn("📋  AUDIT",PURP(),new Color(24,8,44));
            addU.addActionListener(e->doAddUser()); delU.addActionListener(e->doDelUser());
            chgP.addActionListener(e->doChgPw()); clrL.addActionListener(e->doClrLogs());
            rstI.addActionListener(e->doRstIDS()); sysI.addActionListener(e->doSysInfo());
            tglF.addActionListener(e->doToggleFW()); audt.addActionListener(e->doAudit());
            for(JButton b:new JButton[]{addU,delU,chgP,clrL,rstI,sysI,tglF,audt}){ b.setPreferredSize(new Dimension(140,34)); btnRow.add(b); }
            outputArea=new JTextArea(8,0); outputArea.setBackground(INPUT()); outputArea.setForeground(GREEN()); outputArea.setFont(MONO_SM); outputArea.setEditable(false); outputArea.setText("[ Admin console ready ]");
            JScrollPane os=new JScrollPane(outputArea); os.setBorder(new GlowBorder(RED(),1)); styleScroll(os,RED());
            JPanel oc=card("ADMIN CONSOLE OUTPUT",RED()); oc.add(os,BorderLayout.CENTER);
            JSplitPane split=new JSplitPane(JSplitPane.VERTICAL_SPLIT,uc,oc); split.setBorder(null); split.setDividerSize(5); split.setResizeWeight(0.5); split.setPreferredSize(new Dimension(0,500));
            add(title,BorderLayout.NORTH); add(btnRow,BorderLayout.CENTER); add(split,BorderLayout.SOUTH);
        }
        private void refreshTable(){ userModel.setRowCount(0); for(User u:viewer.getBackend().getAuthManager().getAllUsers()) userModel.addRow(new Object[]{u.getUserId(),u.getUsername(),u.getRole().name(),u.getEmail(),u.getLoginCount(),u.isLoggedIn()?"● YES":"○ NO"}); }
        private void out(String t,Color c){ outputArea.setForeground(c); outputArea.setText(t); }
        private JLabel dl(String t){ JLabel l=new JLabel(t); l.setFont(LABEL_F); l.setForeground(DIM()); return l; }
        private void doAddUser(){
            JDialog dlg=new JDialog(viewer.getMainFrame(),"Add New User",true); dlg.setSize(420,360); dlg.setLocationRelativeTo(viewer.getMainFrame()); dlg.getContentPane().setBackground(PANEL()); dlg.setLayout(new GridBagLayout());
            GridBagConstraints gc=new GridBagConstraints(); gc.insets=new Insets(5,12,5,12); gc.fill=GridBagConstraints.HORIZONTAL; gc.gridx=0; gc.weightx=1;
            JTextField unF=styledField(BLUE()); JPasswordField pwF=styledPass(BLUE()); JTextField emF=styledField(BLUE());
            JComboBox<BackendEngine.UserRole> rb=new JComboBox<>(BackendEngine.UserRole.values()); rb.setBackground(CARD()); rb.setForeground(GREEN()); rb.setFont(MONO_MD);
            JLabel stl=new JLabel(" "); stl.setFont(MONO_SM); stl.setForeground(RED());
            int y=0;
            for(Object[] row:new Object[][]{{dl("Username:"),null},{unF,null},{dl("Password (min 8):"),null},{pwF,null},{dl("Email:"),null},{emF,null},{dl("Role:"),null},{rb,null},{stl,null}}){
                gc.gridy=y++;dlg.add((Component)row[0],gc);
            }
            gc.gridy=y; JButton create=cyberBtn("CREATE USER",GREEN(),new Color(0,36,18));
            create.addActionListener(e->{ String un=unF.getText().trim(),pw=new String(pwF.getPassword()),em=emF.getText().trim();
                BackendEngine.UserRole role=(BackendEngine.UserRole)rb.getSelectedItem();
                boolean ok=viewer.getBackend().getAuthManager().registerUser(un,pw,role,em);
                if(ok){ out("User '"+un+"' created — Role: "+role,GREEN()); refreshTable(); dash.refreshStats(); dlg.dispose(); }
                else stl.setText(" ✘  Failed: user exists or password < 8 chars"); });
            dlg.add(create,gc); dlg.setVisible(true);
        }
        private void doDelUser(){
            int row=userTable.getSelectedRow(); if(row<0){ out("Select a user first.",YELL()); return; }
            String un=(String)userModel.getValueAt(row,1);
            if(JOptionPane.showConfirmDialog(this,"Delete '"+un+"'?","Confirm",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){
                boolean ok=viewer.getBackend().getAuthManager().deleteUser(un);
                out(ok?"User '"+un+"' deleted.":"Cannot delete: protected or active.",ok?GREEN():RED());
                if(ok){ refreshTable(); dash.refreshStats(); }
            }
        }
        private void doChgPw(){
            int row=userTable.getSelectedRow(); if(row<0){ out("Select a user first.",YELL()); return; }
            String un=(String)userModel.getValueAt(row,1);
            JPasswordField op=styledPass(BLUE()),np=styledPass(BLUE());
            if(JOptionPane.showConfirmDialog(this,new Object[]{"Old Password:",op,"New Password:",np},"Change PW: "+un,JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION){
                boolean ok=viewer.getBackend().getAuthManager().changePassword(un,new String(op.getPassword()),new String(np.getPassword()));
                out(ok?"Password changed for '"+un+"'.":"Failed: wrong current pw or too short.",ok?GREEN():RED());
            }
        }
        private void doClrLogs(){ User u=viewer.getCurrentUser();
            if(u instanceof Admin){ List<String> arc=((Admin)u).clearLogs(viewer.getBackend().getSecurityLog()); out("Logs cleared. "+arc.size()+" entries archived.",YELL()); dash.refreshStats(); }
        }
        private void doRstIDS(){ viewer.getBackend().getSimulationController().getDetector().reset(); out("IDS reset — alert count and history cleared.",CYAN()); dash.refreshStats(); }
        private void doSysInfo(){ Runtime rt=Runtime.getRuntime(); long used=(rt.totalMemory()-rt.freeMemory())/1048576,total=rt.totalMemory()/1048576;
            out(String.format("=== SYSTEM INFO ===\nJVM     : %s\nJava    : %s\nOS      : %s %s\nMemory  : %dMB / %dMB\nCPUs    : %d\nVersion : %s\nLog     : %s\nUsers   : %d\nSims    : %d\nFW Rules: %d\nAlerts  : %d",
                System.getProperty("java.vm.name"),System.getProperty("java.version"),System.getProperty("os.name"),System.getProperty("os.arch"),
                used,total,rt.availableProcessors(),BackendEngine.VERSION,viewer.getBackend().getSecurityLog().getLogPath(),
                viewer.getBackend().getAuthManager().getUserCount(),viewer.getBackend().getSimulationController().getResultHistory().size(),
                viewer.getBackend().getSimulationController().getFirewall().getRules().size(),
                viewer.getBackend().getSimulationController().getDetector().getAlertCount()),GREEN()); }
        private void doToggleFW(){ Firewall fw=viewer.getBackend().getSimulationController().getFirewall(); fw.setEnabled(!fw.isEnabled());
            out("Firewall "+(fw.isEnabled()?"ENABLED":"DISABLED")+".",fw.isEnabled()?GREEN():RED());
            viewer.getBackend().getSecurityLog().writeLog("WARNING","Firewall toggled: "+(fw.isEnabled()?"ENABLED":"DISABLED")); }
        private void doAudit(){ List<String> trail=viewer.getBackend().getAuthManager().getAuditTrail();
            StringBuilder sb=new StringBuilder("=== ADMIN AUDIT TRAIL ===\n\n");
            if(trail.isEmpty()) sb.append("No events.\n"); else for(String e:trail) sb.append(e).append("\n");
            out(sb.toString(),PURP()); }
    }
}
