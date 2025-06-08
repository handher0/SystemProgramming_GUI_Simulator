package SP25_simulator.src;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * VisualSimulator는 사용자와의 상호작용을 담당한다. 즉, 버튼 클릭등의 이벤트를 전달하고 그에 따른 결과값을 화면에 업데이트
 * 하는 역할을 수행한다.
 * 실제적인 작업은 SicSimulator에서 수행하도록 구현한다.
 */

public class VisualSimulator extends JFrame {
	ResourceManager resourceManager = new ResourceManager();
	SicLoader sicLoader = new SicLoader(resourceManager);
	SicSimulator sicSimulator = new SicSimulator(resourceManager);

	// 명령어 목록 관련 필드 추가
	private final DefaultListModel<String> instructionListModel = new DefaultListModel<>();
	private final JList<String> instructionList = new JList<>(instructionListModel);
    // oneStep 버튼을 클래스 필드로 선언
    private final JButton stepButton = new JButton("실행(1step)");
    // allButton 필드 추가
    private final JButton allButton = new JButton("실행(all)");
    // 파일 이름 입력 필드
    private final JTextField fileNameField = new JTextField(30);

    // 레지스터 패널 내부의 각 JTextField를 추적할 수 있는 배열 선언
    private final JTextField[][] registerFields = new JTextField[10][2];
    private final JTextArea JTextAreaLog = new JTextArea(5, 80);
    private final JTextField deviceField = new JTextField(10);
    private final JTextField targetAddrField = new JTextField(10);

	public VisualSimulator() {
		setTitle("SIC/XE 시뮬레이터");
		setSize(900, 700);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());

		// 상단 파일 로드 패널
		JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		topPanel.add(new JLabel("FileName: "));
		JButton openButton = new JButton("open");
		topPanel.add(fileNameField);
		topPanel.add(openButton);

		openButton.addActionListener(e -> {
			JFileChooser fileChooser = new JFileChooser();
			int result = fileChooser.showOpenDialog(this);
			if (result == JFileChooser.APPROVE_OPTION) {
				File selectedFile = fileChooser.getSelectedFile();
				fileNameField.setText(selectedFile.getAbsolutePath());
				load(selectedFile);
			}
		});
		add(topPanel, BorderLayout.NORTH);

		// 중앙 정보 영역 (왼쪽 + 오른쪽)
		JPanel centerPanel = new JPanel(new GridLayout(1, 2));
		add(centerPanel, BorderLayout.CENTER);

		// 왼쪽 패널 (H 레코드 + 레지스터)
		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

		// H 레코드 패널
		JPanel hPanel = new JPanel();
		hPanel.setLayout(new BoxLayout(hPanel, BoxLayout.Y_AXIS));
		hPanel.setBorder(BorderFactory.createTitledBorder("H (Header Record)"));
		hPanel.add(new JLabel("Program name:"));
		hPanel.add(new JTextField(10));
		hPanel.add(new JLabel("Start Address of Object Program:"));
		hPanel.add(new JTextField(10));
		hPanel.add(new JLabel("Length of Program:"));
		hPanel.add(new JTextField(10));
		leftPanel.add(hPanel);

		// 레지스터 패널
		JPanel registerPanel = new JPanel();
		registerPanel.setLayout(new BoxLayout(registerPanel, BoxLayout.Y_AXIS));
		registerPanel.setBorder(BorderFactory.createTitledBorder("Register"));

        String[] allRegs = {"A", "X", "L", "B", "S", "T", "F", "PC", "SW"};
        JPanel allRegPanel = new JPanel(new GridLayout(allRegs.length, 3));
        for (String name : allRegs) {
            int regIdx = getRegisterIndex(name);
            allRegPanel.add(new JLabel(name + " (#" + regIdx + ")"));
            if (name.equals("SW") || name.equals("F")) {
                allRegPanel.add(new JLabel(""));
                JTextField hexField = new JTextField(6); // Hex만
                allRegPanel.add(hexField);
                registerFields[regIdx][0] = null;
                registerFields[regIdx][1] = hexField;
            } else {
                JTextField decField = new JTextField(6); // Dec
                JTextField hexField = new JTextField(6); // Hex
                allRegPanel.add(decField);
                allRegPanel.add(hexField);
                registerFields[regIdx][0] = decField;
                registerFields[regIdx][1] = hexField;
            }
        }
        registerPanel.add(allRegPanel);

		leftPanel.add(registerPanel);
		centerPanel.add(leftPanel);

		// 오른쪽 패널 (E 레코드 + Instructions)
		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));

		// E 레코드 패널
		JPanel ePanel = new JPanel();
		ePanel.setLayout(new BoxLayout(ePanel, BoxLayout.Y_AXIS));
		ePanel.setBorder(BorderFactory.createTitledBorder("E (End Record)"));
		ePanel.add(new JLabel("Address of First Instruction in Object Program:"));
		ePanel.add(new JTextField(10));
		rightPanel.add(ePanel);

		rightPanel.add(new JLabel("Start Address in Memory:"));
		rightPanel.add(new JTextField(10));
		rightPanel.add(new JLabel("Target Address:"));
		rightPanel.add(targetAddrField);
		// --- Instructions 및 버튼 패널 ---
		JPanel execPanel = new JPanel();
		execPanel.setLayout(new BoxLayout(execPanel, BoxLayout.Y_AXIS));
		execPanel.setBorder(BorderFactory.createTitledBorder("Instructions"));
		execPanel.add(new JLabel("Instructions:"));
		execPanel.add(new JScrollPane(instructionList));
		execPanel.add(new JLabel("사용중인 장치:"));
		execPanel.add(deviceField);

		stepButton.addActionListener(e -> oneStep());
		// Disable step button by default, immediately after creation
		stepButton.setEnabled(false);
		allButton.addActionListener(e -> allStep());
		// Disable allButton by default, immediately after creation
		allButton.setEnabled(false);
		JButton exitButton = new JButton("종료");
		exitButton.addActionListener(e -> System.exit(0));
		execPanel.add(stepButton);
		execPanel.add(allButton);
		execPanel.add(exitButton);
		rightPanel.add(execPanel);

		centerPanel.add(rightPanel);

		// 하단 로그 출력
		JPanel bottomPanel = new JPanel(new BorderLayout());
		bottomPanel.setBorder(BorderFactory.createTitledBorder("Log (명령어 수행 관련) :"));
		JTextAreaLog.setEditable(false);
		bottomPanel.add(new JScrollPane(JTextAreaLog), BorderLayout.CENTER);
		add(bottomPanel, BorderLayout.SOUTH);

		// Refresh layout and UI
		revalidate();
		repaint();
	}

	private int getRegisterIndex(String name) {
		return switch (name) {
			case "A" -> 0;
			case "X" -> 1;
			case "L" -> 2;
			case "B" -> 3;
			case "S" -> 4;
			case "T" -> 5;
			case "F" -> 6;
			case "PC" -> 8;
			case "SW" -> 9;
			default -> -1;
		};
	}


	/**
	 * 프로그램 로드 명령을 전달한다.
	 */
	public void load(File program) {
	    if (!program.exists()) {
	        JOptionPane.showMessageDialog(this, "[지정된 파일을 찾을 수 없습니다.]\n" + program.getPath(),
	                "파일 없음", JOptionPane.WARNING_MESSAGE);
	        return;
	    }

	    fileNameField.setText(program.getName());
		sicLoader.setSicSimulator(sicSimulator);
		sicSimulator.openInstFile("inst_table.txt");
	    sicSimulator.load(program);
	    sicLoader.load(program);
		for (var entry : sicSimulator.instMap.entrySet()) {
			Instruction i = entry.getValue();
		}
	    init();
	    // Enable step and all buttons after loading
	    stepButton.setEnabled(true);
	    allButton.setEnabled(true);
	}

	/**
	 * 하나의 명령어만 수행할 것을 SicSimulator에 요청한다.
	 */
	public void oneStep() {
	    sicSimulator.oneStep();
	    update();
	}

	/**
	 * 남아있는 모든 명령어를 수행할 것을 SicSimulator에 요청한다.
	 * update()가 각 oneStep() 실행 후 호출되어 로그가 실시간으로 갱신됨.
	 */
	public void allStep() {
	    sicSimulator.allStep(this::update);
	}

    /**
     * 화면을 최신값으로 갱신하는 역할을 수행한다.
     */
    public void update() {
        // 현재 PC에 해당하는 명령어를 리스트에서 강조
        int executedAddr = sicSimulator.currAddr;
        if (executedAddr != -1) {  // 프로그램 종료 시 하이라이팅 방지
            for (int i = 0; i < instructionListModel.size(); i++) {
                String entry = instructionListModel.get(i);
                if (entry.startsWith(String.format("%06X", executedAddr))) {
                    instructionList.setSelectedIndex(i);
                    instructionList.ensureIndexIsVisible(i);
                    break;
                }
            }
        } else {
            instructionList.clearSelection(); // 명시적으로 선택 제거
            instructionList.ensureIndexIsVisible(0); // 목록을 맨 위로 스크롤
            // oneStep 버튼 비활성화
            stepButton.setEnabled(false);
            // 실행(all) 버튼도 비활성화
            allButton.setEnabled(false);
        }

        // 레지스터 값 업데이트
        String[] regNames = {"A", "X", "L", "B", "S", "T", "F", "", "PC", "SW"};
        for (String regName : regNames) {
            if (regName.isEmpty()) continue;
            int regIndex = getRegisterIndex(regName);
            int value = resourceManager.getRegister(regIndex);
            if (registerFields[regIndex][0] != null) {
                int signed24bit = value;
                if ((value & 0x800000) != 0) {  // if sign bit is set
                    signed24bit |= 0xFF000000; // extend to 32-bit negative
                }
                registerFields[regIndex][0].setText(String.valueOf(signed24bit));
            }
            if (registerFields[regIndex][1] != null) {
                registerFields[regIndex][1].setText(String.format("%06X", value));
            }
        }

        // 로그 출력
        if (!sicSimulator.logList.isEmpty()) {
            String log = sicSimulator.logList.getLast();
            if (JTextAreaLog.getDocument().getLength() == 0)
                JTextAreaLog.append(log);
            else
                JTextAreaLog.append('\n' + log);
            JTextAreaLog.setCaretPosition(JTextAreaLog.getDocument().getLength());
        }

        String devName = sicSimulator.inst.currDevice;
        if (devName != null && !devName.isBlank()) {
            deviceField.setText(devName);
        } else {
            deviceField.setText("");
        }

        // --- Target Address Field Update Logic ---
        int format = sicSimulator.lastFormat;
        int nixbpe = sicSimulator.lastNixbpe;
        int disp = sicSimulator.lastDisp;
        boolean isImmediate = ((nixbpe >> 5) & 1) == 0 && ((nixbpe >> 4) & 1) == 1;

        if ((format == 3 || format == 4) && !isImmediate && sicSimulator.lastOpcode != 0x4C) {
            int targetAddr;
            boolean isExtended = (nixbpe & 0x01) == 1;  // e == 1
            boolean isIndexed = ((nixbpe >> 3) & 1) == 1; // x == 1

            if (isExtended) {
                targetAddr = disp;
            } else {
                int nextPc = sicSimulator.currAddr + sicSimulator.lastFormat;
                targetAddr = nextPc + disp;
            }

            if (isIndexed) {
                int xValue = resourceManager.getRegister(1); // X 레지스터는 index 1
                targetAddr += xValue;
            }

            targetAddrField.setText(String.format("%04X", targetAddr & 0xFFFF));
        } else {
            targetAddrField.setText("");
        }

        if (sicSimulator.currAddr == -1) {
            targetAddrField.setText("");
        }
    }

	/**
	 * 현재 로딩된 파일을 기준으로 GUI 구성요소나 레지스터를 초기화/갱신하는 메서드
	 */
	private void init() {
		// 추후 정보 초기화 코드 삽입 가능
		// 예: 레지스터 필드 초기화, 현재 포인터 정보 표시 등
		instructionListModel.clear();

        // H 레코드 정보 초기화
        JPanel hPanel = (JPanel) ((JPanel) ((JPanel) getContentPane().getComponent(1)).getComponent(0)).getComponent(0);
        ((JTextField) hPanel.getComponent(1)).setText(sicLoader.programName);
        ((JTextField) hPanel.getComponent(3)).setText(String.format("%06X", sicLoader.startAddress));
        ((JTextField) hPanel.getComponent(5)).setText(String.format("%04X", sicLoader.totalLength));

        // E 레코드 정보 초기화
        JPanel rightPanel = (JPanel) ((JPanel) getContentPane().getComponent(1)).getComponent(1);
        JPanel ePanel = (JPanel) rightPanel.getComponent(0);
        ((JTextField) ePanel.getComponent(1)).setText(String.format("%06X", sicLoader.startAddress));
        // "Start Address in Memory" 필드도 0으로 초기화
        ((JTextField) rightPanel.getComponent(2)).setText("0");

		// 레지스터 필드 초기화
		for (int i = 0; i < registerFields.length; i++) {
		    if (registerFields[i][0] != null) {
		        if (i == getRegisterIndex("L")) {
		            registerFields[i][0].setText(String.valueOf(-1));
		        } else {
		            registerFields[i][0].setText("0");
		        }
		    }
		    if (registerFields[i][1] != null) {
		        if (i == getRegisterIndex("L")) {
		            registerFields[i][1].setText("FFFFFF");
		        } else {
		            registerFields[i][1].setText("000000");
		        }
		    }
		}

		for (String inst : sicLoader.getInstructionList()) {
			instructionListModel.addElement(inst);
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			VisualSimulator gui = new VisualSimulator();
			gui.setVisible(true); // 창 보이게 만들기
		});
	}
}
