package SP25_simulator.src;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * 시뮬레이터로서의 작업을 담당한다. VisualSimulator에서 사용자의 요청을 받으면 이에 따라 ResourceManager에 접근하여
 * 작업을 수행한다.
 *
 * 작성중의 유의사항 : 1) 새로운 클래스, 새로운 변수, 새로운 함수 선언은 얼마든지 허용됨. 단, 기존의 변수와 함수들을 삭제하거나
 * 완전히 대체하는 것은 지양할 것. 2) 필요에 따라 예외처리, 인터페이스 또는 상속 사용 또한 허용됨. 3) 모든 void 타입의 리턴값은
 * 유저의 필요에 따라 다른 리턴 타입으로 변경 가능. 4) 파일, 또는 콘솔창에 한글을 출력시키지 말 것. (채점상의 이유. 주석에 포함된
 * 한글은 상관 없음)
 *
 *
 *
 * + 제공하는 프로그램 구조의 개선방법을 제안하고 싶은 분들은 보고서의 결론 뒷부분에 첨부 바랍니다. 내용에 따라 가산점이 있을 수
 * 있습니다.
 */
public class SicSimulator {
	int currAddr; // 현재 실행한 명령어의 시작 주소
	ResourceManager rMgr;
	InstLuncher inst;
	ArrayList<String> logList = new ArrayList<>();
	HashMap<Integer, Instruction> instMap = new HashMap<>();
	boolean running = true;
	public int currentDevice = -1;

	public int lastOpcode;
	public int lastNixbpe;
	public int lastDisp;
	public int lastFormat;

	public SicSimulator(ResourceManager resourceManager) {
		// 필요하다면 초기화 과정 추가
		this.rMgr = resourceManager;
	}

	/**
	 * 레지스터, 메모리 초기화 등 프로그램 load와 관련된 작업 수행. 단, object code의 메모리 적재 및 해석은
	 * SicLoader에서 수행하도록 한다.
	 */
	public void load(File program) {
		/* 메모리 초기화, 레지스터 초기화 등 */
		try {
			// 1. 리소스 초기화 (메모리 + 레지스터 + SYMTAB)
			rMgr.initializeResource();
			// 2. 로그 초기화
			logList.clear();
			// 3. InstLuncher 새로 생성
			inst = new InstLuncher(rMgr);
			running = true;

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 1개의 instruction이 수행된 모습을 보인다.
	 */
	public void oneStep() {
	    int pc = rMgr.getRegister(ResourceManager.REG_PC); // 현재 PC

	    currAddr = pc; // 실행한 명령어의 주소 저장
	    int locctr = pc; // 명령어 시작 주소
	    char[] memoryBytes = rMgr.getMemory(pc, 4); // 최대 4바이트 읽기
	    int byte1 = memoryBytes[0] & 0xFF;
	    int opcode = byte1 & 0xFC; // 상위 6비트
	    Instruction inst = instMap.get(opcode);

		if (inst == null) {
			// ← 여기에 디버깅 코드 추가
			char[] memBytes = rMgr.getMemory(pc, 4);
			System.out.printf("Memory dump at %06X: %02X %02X %02X %02X%n",
					pc,
					(int) memBytes[0] & 0xFF,
					(int) memBytes[1] & 0xFF,
					(int) memBytes[2] & 0xFF,
					(int) memBytes[3] & 0xFF
			);

			addLog(String.format("Unknown instruction at %06X: %02X", pc, byte1));
			rMgr.setRegister(ResourceManager.REG_PC, pc + 1); // 오류 회피용 증가
			return;
		}

	    int format = inst.format;
	    int instLen = 0;
	    int nixbpe = 0;
	    int disp = 0;

	    if (format == 1) {
	        instLen = 1;
	    } else if (format == 2) {
	        instLen = 2;
	        disp = memoryBytes[1] & 0xFF; // byte2 전체를 넘겨야 r1, r2를 비트 연산으로 추출 가능
	    } else if (format == 3 || format == 4) {
	        boolean isExtended = (memoryBytes[1] & 0x10) != 0;
	        instLen = isExtended ? 4 : 3;

	        // Corrected nixbpe extraction
	        int n = (memoryBytes[0] & 0x02) >> 1;
	        int i = (memoryBytes[0] & 0x01);
	        int x = (memoryBytes[1] & 0x80) >> 7;
	        int b = (memoryBytes[1] & 0x40) >> 6;
	        int p = (memoryBytes[1] & 0x20) >> 5;
	        int e = (memoryBytes[1] & 0x10) >> 4;
	        nixbpe = (n << 5) | (i << 4) | (x << 3) | (b << 2) | (p << 1) | e;

	        if (isExtended) {
	            disp = ((memoryBytes[2] & 0x3F) << 8) | (memoryBytes[3] & 0xFF);
	        } else {
	            disp = ((memoryBytes[2] & 0xFF) | ((memoryBytes[1] & 0x0F) << 8));
	            if ((disp & 0x800) != 0) disp |= 0xFFFFF000; // sign-extend
	        }
	    }

	    lastOpcode = opcode;
	    lastNixbpe = nixbpe;
	    lastDisp = disp;
	    lastFormat = format;

	    // 명령어 실행
	    int pcAfterInst = pc + instLen;
	    Integer updatedPC = inst.format >= 3
	            ? this.inst.execute(inst.instruction, nixbpe, disp, pcAfterInst)
	            : this.inst.execute(inst.instruction, nixbpe, disp, 0);

	    if (updatedPC != null) {
	        rMgr.setRegister(ResourceManager.REG_PC, updatedPC);
	    } else {
	        rMgr.setRegister(ResourceManager.REG_PC, pc + instLen);
	    }

	    currentDevice = -1;

	    addLog(String.format("Executed %s at %06X", inst.instruction, pc));

	    if (rMgr.getRegister(ResourceManager.REG_PC) == 0xFFFFFF) {
	        addLog(String.format("Executed %s at %06X", inst.instruction, pc));
	        currAddr = -1;
	        running = false;
	    }
	}

	/**
	 * 남은 모든 instruction이 수행된 모습을 보인다.
	 */
	public void allStep(Runnable updateCallback) {
	    while (running) {
	        oneStep();
	        updateCallback.run();
	    }
	}


	/**
	 * 각 단계를 수행할 때 마다 관련된 기록을 남기도록 한다.
	 */
	public void addLog(String log) {
		logList.add(log);
	}

	public void openInstFile(String fileName) {
		try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
			String line;
			while ((line = br.readLine()) != null) {
				Instruction instruction = new Instruction(line);
				// "-" 는 지시어이므로 무시
				if (!instruction.opcode.equals("-")) {
					int maskedOpcode = Integer.parseInt(instruction.opcode, 16) & 0xFC;
					instMap.put(maskedOpcode, instruction);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

class Instruction {
	public String instruction;
	public int format;
	public String opcode;
	public int operandCount;

	public Instruction(String instruction, int format, String opcode, int operandCount) {
		this.instruction = instruction;
		this.format = format;
		this.opcode = opcode;
		this.operandCount = operandCount;
	}

	public Instruction(String line) {
		String[] parts = line.trim().split("\\|");
		if (parts.length >= 5) {
			this.instruction = parts[1].trim(); // ex: ADD
			this.format = parts[2].trim().equals("-") ? -1 : Integer.parseInt(parts[2].trim());
			this.opcode = parts[3].trim(); // ex: 18
			this.operandCount = parts[4].trim().isEmpty() ? 0 : Integer.parseInt(parts[4].trim());
		}
	}
}