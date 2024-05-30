package projekt.o_x;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class o_x implements Runnable {

	private String ip = "";
	private int port;
	private Scanner scanner = new Scanner(System.in);
	private JFrame frame;
	private final int szerokosc = 506;
	private final int wysokosc = 527;
	private Thread thread;

	private Painter painter;
	private Socket socket;
	private DataOutputStream dos;
	private DataInputStream dis;

	private ServerSocket serverSocket;

	private BufferedImage plansza;
	private BufferedImage x;
	private BufferedImage o;
	

	private String[] odstepy = new String[9];
	private boolean kolej = false;
	private boolean akcje = true;
	private boolean akceptacja = false;
	private boolean blad_polaczenia = false;
	private boolean wygrana = false;
	private boolean przegrana = false;
	private boolean remis = false;

	private int dlugosc = 160;
	private int bledy = 0;
	private int wynik1 = -1;
	private int wynik2 = -1;

	private Font czcionka_1 = new Font("Cascadia Mono", Font.BOLD, 25);
	private Font czcionka_2 = new Font("Cascadia Mono", Font.BOLD, 15);
	private Font czcionka_3 = new Font("Cascadia Mono", Font.BOLD, 60);

	private String oczekiwanie = "Oczekiwanie na przeciwnika";
	private String blad_p = "Błąd połączenia";
	private String zwyciestwo = "Wygrałeś!";
	private String porazka = "Przegrałeś!";
	private String remiss = "Remis!";

	private int[][] wygrane = new int[][] { { 0, 1, 2 }, { 3, 4, 5 }, { 6, 7, 8 }, { 0, 3, 6 }, { 1, 4, 7 }, { 2, 5, 8 }, { 0, 4, 8 }, { 2, 4, 6 } };

	public o_x() {
		System.out.println("Podaj adres IP: ");
		ip = scanner.nextLine();
		System.out.println("Podaj port: ");
		port = scanner.nextInt();
		while (port < 1 || port > 65535) {
			System.out.println("Podany port jest błędny, podaj inny: ");
			port = scanner.nextInt();
		}

		zaladuj_obiekty();

		painter = new Painter();
		painter.setPreferredSize(new Dimension(szerokosc, wysokosc));

		if (!polaczenie()) stworz_serwer();

		frame = new JFrame();
		frame.setTitle("Kółko i Krzyżyk");
		frame.setContentPane(painter);
		frame.setSize(szerokosc, wysokosc);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false);
		frame.setVisible(true);

		thread = new Thread(this, "Kółko i Krzyżyk");
		thread.start();
	}

	public void run() {
		while (true) {
			start();
			painter.repaint();
			if (!akcje && !akceptacja) {
				sprawdzaj_serwer();
			}

		}
	}

	private void renderuj(Graphics g) {
		g.drawImage(plansza, 0, 0, null);
		if (blad_polaczenia) {
			g.setColor(Color.RED);
			g.setFont(czcionka_2);
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			int stringszerokosc = g2.getFontMetrics().stringWidth(blad_p);
			g.drawString(blad_p, szerokosc / 2 - stringszerokosc / 2, wysokosc / 2);
			return;
		}

		if (akceptacja) {
			for (int i = 0; i < odstepy.length; i++) {
				if (odstepy[i] != null) {
					if (odstepy[i].equals("X")) {
						if (akcje) {
							g.drawImage(x, (i % 3) * dlugosc + 10 * (i % 3), (int) (i / 3) * dlugosc + 10 * (int) (i / 3), null);
						} else {
							g.drawImage(x, (i % 3) * dlugosc + 10 * (i % 3), (int) (i / 3) * dlugosc + 10 * (int) (i / 3), null);
						}
					} else if (odstepy[i].equals("O")) {
						if (akcje) {
							g.drawImage(o, (i % 3) * dlugosc + 10 * (i % 3), (int) (i / 3) * dlugosc + 10 * (int) (i / 3), null);
						} else {
							g.drawImage(o, (i % 3) * dlugosc + 10 * (i % 3), (int) (i / 3) * dlugosc + 10 * (int) (i / 3), null);
						}
					}
				}
			}
			if (wygrana || przegrana) {
				Graphics2D g2 = (Graphics2D) g;
				
				g.setColor(Color.RED);
				g.setFont(czcionka_3);
				if (wygrana) {
					int stringszerokosc = g2.getFontMetrics().stringWidth(zwyciestwo);
					g.drawString(zwyciestwo, szerokosc / 2 - stringszerokosc / 2, wysokosc / 2);
				} else if (przegrana) {
					int stringszerokosc = g2.getFontMetrics().stringWidth(porazka);
					g.drawString(porazka, szerokosc / 2 - stringszerokosc / 2, wysokosc / 2);
				}
			}
			if (remis) {
				Graphics2D g2 = (Graphics2D) g;
				g.setColor(Color.BLACK);
				g.setFont(czcionka_3);
				int stringszerokosc = g2.getFontMetrics().stringWidth(remiss);
				g.drawString(remiss, szerokosc / 2 - stringszerokosc / 2, wysokosc / 2);
			}
		} else {
			g.setColor(Color.RED);
			g.setFont(czcionka_1);
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			int stringszerokosc = g2.getFontMetrics().stringWidth(oczekiwanie);
			g.drawString(oczekiwanie, szerokosc / 2 - stringszerokosc / 2, wysokosc / 2);
		}

	}

	private void start() {
		if (bledy >= 10) blad_polaczenia = true;

		if (!kolej && !blad_polaczenia) {
			try {
				int odstep = dis.readInt();
				if (akcje) odstepy[odstep] = "X";
				else odstepy[odstep] = "O";
				sprawdz_wygrana_przeciwnika();
				sprawdz_remis();
				kolej = true;
			} catch (IOException e) {
				e.printStackTrace();
				bledy++;
			}
		}
	}

	private void sprawdz_wygrana() {
		for (int i = 0; i < wygrane.length; i++) {
			if (akcje) {
				if (odstepy[wygrane[i][0]] == "O" && odstepy[wygrane[i][1]] == "O" && odstepy[wygrane[i][2]] == "O") {
					wynik1 = wygrane[i][0];
					wynik2 = wygrane[i][2];
					wygrana = true;
				}
			} else {
				if (odstepy[wygrane[i][0]] == "X" && odstepy[wygrane[i][1]] == "X" && odstepy[wygrane[i][2]] == "X") {
					wynik1 = wygrane[i][0];
					wynik2 = wygrane[i][2];
					wygrana = true;
				}
			}
		}
	}

	private void sprawdz_wygrana_przeciwnika() {
		for (int i = 0; i < wygrane.length; i++) {
			if (akcje) {
				if (odstepy[wygrane[i][0]] == "X" && odstepy[wygrane[i][1]] == "X" && odstepy[wygrane[i][2]] == "X") {
					wynik1 = wygrane[i][0];
					wynik2 = wygrane[i][2];
					przegrana = true;
				}
			} else {
				if (odstepy[wygrane[i][0]] == "O" && odstepy[wygrane[i][1]] == "O" && odstepy[wygrane[i][2]] == "O") {
					wynik1 = wygrane[i][0];
					wynik2 = wygrane[i][2];
					przegrana = true;
				}
			}
		}
	}

	private void sprawdz_remis() {
		for (int i = 0; i < odstepy.length; i++) {
			if (odstepy[i] == null) {
				return;
			}
		}
		remis = true;
	}

	private void sprawdzaj_serwer() {
		Socket socket = null;
		try {
			socket = serverSocket.accept();
			dos = new DataOutputStream(socket.getOutputStream());
			dis = new DataInputStream(socket.getInputStream());
			akceptacja = true;
			System.out.println("Klient się połączył");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private boolean polaczenie() {
		try {
			socket = new Socket(ip, port);
			dos = new DataOutputStream(socket.getOutputStream());
			dis = new DataInputStream(socket.getInputStream());
			akceptacja = true;
		} catch (IOException e) {
			System.out.println("Błąd połączenia z: " + ip + ":" + port + " | Tworzę serwer");
			return false;
		}
		System.out.println("Pomyślnie polączono z serwerem");
		return true;
	}

	private void stworz_serwer() {
		try {
			serverSocket = new ServerSocket(port, 8, InetAddress.getByName(ip));
		} catch (Exception e) {
			e.printStackTrace();
		}
		kolej = true;
		akcje = false;
	}

	private void zaladuj_obiekty() {
		try {
			plansza = ImageIO.read(getClass().getResourceAsStream("/plansza.png"));
			x = ImageIO.read(getClass().getResourceAsStream("/x.png"));
			o = ImageIO.read(getClass().getResourceAsStream("/o.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unused")
	public static void main(String[] args) {
		o_x O_X = new o_x();
	}

	private class Painter extends JPanel implements MouseListener {
		private static final long serialVersionUID = 1L;

		public Painter() {
			setFocusable(true);
			requestFocus();
			setBackground(Color.WHITE);
			addMouseListener(this);
		}

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			renderuj(g);
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if (akceptacja) {
				if (kolej && !blad_polaczenia && !wygrana && !przegrana) {
					int x = e.getX() / dlugosc;
					int y = e.getY() / dlugosc;
					y *= 3;
					int position = x + y;

					if (odstepy[position] == null) {
						if (!akcje) odstepy[position] = "X";
						else odstepy[position] = "O";
						kolej = false;
						repaint();
						Toolkit.getDefaultToolkit().sync();

						try {
							dos.writeInt(position);
							dos.flush();
						} catch (IOException e1) {
							bledy++;
							e1.printStackTrace();
						}

						System.out.println("wysłano dane");
						sprawdz_wygrana();
						sprawdz_remis();

					}
				}
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {

		}

		@Override
		public void mouseReleased(MouseEvent e) {

		}

		@Override
		public void mouseEntered(MouseEvent e) {

		}

		@Override
		public void mouseExited(MouseEvent e) {

		}

	}

}
