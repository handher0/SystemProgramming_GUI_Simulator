package SP25_simulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * SicLoader는 프로그램을 해석해서 메모리에 올리는 역할을 수행한다. 이 과정에서 linker의 역할 또한 수행한다.
 *
 * SicLoader가 수행하는 일을 예를 들면 다음과 같다. - program code를 메모리에 적재시키기 - 주어진 공간만큼 메모리에 빈
 * 공간 할당하기 - 과정에서 발생하는 symbol, 프로그램 시작주소, control section 등 실행을 위한 정보 생성 및 관리
 */
public class SicLoader {
	ResourceManager rMgr;
	public String programName;
	public int startAddress;
	public int totalLength;
	public int firstInstruction;
	private List<ModificationRecord> modificationList = new ArrayList<>();
	private SicSimulator sicSimulator;
	private List<String> instructionList = new ArrayList<>();
	private boolean isFirstHeader = true;
	private boolean isFirstEnd = true;
	private int currentSectionStart = 0;
	private int memoryCursor = 0; // 현재까지 메모리에 사용된 위치
	private int totalHeaderLength = 0;


	public SicLoader(ResourceManager resourceManager) {
		// 필요하다면 초기화
		setResourceManager(resourceManager);
	}

	/**
	 * Loader와 프로그램을 적재할 메모리를 연결시킨다.
	 *
	 * @param resourceManager d
	 */
	public void setResourceManager(ResourceManager resourceManager) {
		this.rMgr = resourceManager;
	}

	public void setSicSimulator(SicSimulator sicSimulator) {
		this.sicSimulator = sicSimulator;
	}

	public List<String> getInstructionList() {
		return instructionList;
	}

	/**
	 * object code를 읽어서 load과정을 수행한다. load한 데이터는 resourceManager가 관리하는 메모리에 올라가도록
	 * 한다. load과정에서 만들어진 symbol table 등 자료구조 역시 resourceManager에 전달한다.
	 *
	 * @param objectCode 읽어들인 파일
	 */
	public void load(File objectCode) {
		try (BufferedReader reader = new BufferedReader(new FileReader(objectCode))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isEmpty()) continue;
				switch (line.charAt(0)) {
					case 'H':
						parseHeader(line);
						break;
					case 'D':
						parseDefine(line);
						break;
					case 'R':
						break; // Reference record skipped (not implemented)
					case 'T':
						parseText(line);  // Directly parse and store in memory
						break;
					case 'M':
						parseModify(line); // Just store modification info
						break;
					case 'E':
						parseEnd(line);
						break;
				}
			}
			applyModifications();  // Apply modifications after all text records are processed
			InstructionListFromMemory(); // ui 표시 용
			rMgr.printMemoryDump(0, 256);
			rMgr.printMemoryDump(0x1000, 0x1100);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void parseHeader(String line) {
		String name = line.substring(1, 7).trim();
		int start = Integer.parseInt(line.substring(7, 13), 16);
		int length = Integer.parseInt(line.substring(13, 19), 16);

		int adjustedStart = isFirstHeader ? 0 : totalHeaderLength;
		if (isFirstHeader) {
			currentSectionStart = 0;
		}
		rMgr.symtab.putSymbol(name, adjustedStart);

		if (isFirstHeader) {
			programName = name;
			startAddress = adjustedStart;
			isFirstHeader = false;
		}

		totalLength += length;
		totalHeaderLength += length;
		currentSectionStart = adjustedStart;
	}

	private void parseText(String line) {
		int loc = currentSectionStart + Integer.parseInt(line.substring(1, 7), 16);
		int len = Integer.parseInt(line.substring(7, 9), 16);
		String objCode = line.substring(9);

		int ptr = 0;
		while (ptr < len * 2) {
			if (ptr + 2 > objCode.length()) break;

			// 첫 바이트로부터 상위 6비트만 추출 (opcode)
			int byte1 = Integer.parseInt(objCode.substring(ptr, ptr + 2), 16);
			int byte2 = (ptr + 2 <= objCode.length() - 2) ? Integer.parseInt(objCode.substring(ptr + 2, ptr + 4), 16) : 0;

			int opcode6bit = byte1 & 0xFC;
			Instruction inst = sicSimulator.instMap.get(opcode6bit);
			// 명령어 테이블에 존재하지 않으면 건너뜀
			if (inst == null || inst.format <= 0) {
				ptr += 2; // 최소 단위 1 byte만 이동
				continue;
			}
			int instLen;
			if (inst.format == 1) {
				instLen = 2;
			} else if (inst.format == 2) {
				instLen = 4;
			} else {
				// format 3 or 4
				boolean ebit = (byte2 & 0x10) != 0;
				instLen = ebit ? 8 : 6;
			}
			if (ptr + instLen > objCode.length()) {
				instLen = objCode.length() - ptr; // adjust to remaining length
			}
			String fullInstCode = objCode.substring(ptr, ptr + instLen);
			// 메모리에 저장
			for (int i = 0; i < instLen / 2; i++) {
				String b = fullInstCode.substring(i * 2, i * 2 + 2);
				int val = Integer.parseInt(b, 16);
				int memAddr = loc + (ptr / 2) + i;
				System.out.printf("[MEMORY WRITE] Addr: %06X, Value: %02X\n", memAddr, val);
				rMgr.setMemory(memAddr, new char[]{(char) val}, 1);
			}

			// 명령어만 시각화 리스트에 추가
			System.out.printf("[INSTRUCTION] Addr: %06X, Code: %s\n", loc + ptr / 2, fullInstCode);
//			if (inst != null && inst.format > 0 && !fullInstCode.equalsIgnoreCase("454F46") && !fullInstCode.equalsIgnoreCase("F1") && !fullInstCode.equalsIgnoreCase("000000") && !fullInstCode.equalsIgnoreCase("05")) {
//
//				instructionList.add(String.format("%06X  %s", loc + ptr / 2, fullInstCode));
//			}


			ptr += instLen;
		}

		memoryCursor = Math.max(memoryCursor, loc + len);
	}

	private void parseDefine(String line) {
		int i = 1;
		while (i + 12 <= line.length()) {
			String symbol = line.substring(i, i + 6).trim();
			int address = Integer.parseInt(line.substring(i + 6, i + 12), 16);
			rMgr.symtab.putSymbol(symbol, address);
			System.out.println("[DEFINE] Symbol: " + symbol + ", Address: " + String.format("%06X", address));
			i += 12;
		}
	}

	private void parseModify(String line) {
		int address = Integer.parseInt(line.substring(1, 7), 16);
		int length = Integer.parseInt(line.substring(7, 9), 16);
		char sign = line.charAt(9);
		String symbol = line.substring(10).trim();
		modificationList.add(new ModificationRecord(address, length, sign, symbol, currentSectionStart));
	}

	private void parseEnd(String line) {
		if (isFirstEnd) {
			if (line.length() >= 7) {
				String startAddrStr = line.substring(1, 7);
				firstInstruction = Integer.parseInt(startAddrStr, 16);
			} else {
				firstInstruction = startAddress;
			}
			isFirstEnd = false;
		}
	}
	private void applyModifications() {
		for (ModificationRecord m : modificationList) {
			int fullAddr = m.sectionStart + m.address;
			System.out.printf("[DEBUG] SectionStart: %06X, Addr: %06X, FullAddr: %06X\n", m.sectionStart, m.address, fullAddr);

			char[] raw = rMgr.getMemory(fullAddr, (m.length + 1) / 2); // ceil(nibble/2)
			StringBuilder hexBuilder = new StringBuilder();
			for (char c : raw) {
				hexBuilder.append(String.format("%02X", (int) c & 0xFF));
			}

			String hexStr = hexBuilder.toString();
			int original = Integer.parseInt(hexStr, 16);
			int symAddr = rMgr.symtab.search(m.symbol);
			int result = (m.sign == '+') ? original + symAddr : original - symAddr;

			String resultHex = String.format("%0" + m.length + "X", result);
			char[] resultBytes = new char[(m.length + 1) / 2];
			for (int i = 0; i < resultBytes.length; i++) {
				resultBytes[i] = (char) Integer.parseInt(resultHex.substring(i * 2, i * 2 + 2), 16);
			}

			System.out.println("[MODIFY] Addr: " + String.format("%06X", fullAddr) + ", Len: " + m.length +
					", Orig: " + hexStr + ", Sym: " + m.symbol + ", SymAddr: " + String.format("%06X", symAddr) +
					", Sign: " + m.sign + ", Result: " + resultHex);
			rMgr.setMemory(fullAddr, resultBytes, resultBytes.length);
		}
	}



	private void InstructionListFromMemory() {
		instructionList.clear();
		int addr = startAddress;

		while (addr < memoryCursor) {
			char firstByte = rMgr.getMemory(addr, 1)[0];
			if ((firstByte & 0xFF) == 0x30) {
				addr++;
				continue;
			}

			char[] instBytesPre = rMgr.getMemory(addr, 3); // 최대 3바이트 미리 가져오기
			StringBuilder sbPre = new StringBuilder();
			for (char b : instBytesPre) {
				sbPre.append(String.format("%02X", b & 0xFF));
			}
			String codePre = sbPre.toString();

			if (isExcludedCode(codePre)) {
				addr += 3; // 임의로 3바이트 건너뜀 (실제 format 알기 전이므로 보수적 접근)
				continue;
			}

			char[] bytes = rMgr.getMemory(addr, 3); // 명령어 해석에 필요한 최대 길이
			if (bytes.length < 1) {
				addr++;
				continue;
			}

			int byte1 = bytes[0] & 0xFF;
			int byte2 = (bytes.length > 1) ? (bytes[1] & 0xFF) : 0;
			int opcode6bit = byte1 & 0xFC;
			Instruction inst = sicSimulator.instMap.get(opcode6bit);
			if (inst == null || inst.format <= 0) {
				addr++;
				continue;
			}

			int instLen;
			if (inst.format == 1) {
				instLen = 1;
			} else if (inst.format == 2) {
				instLen = 2;
			} else {
				boolean ebit = (byte2 & 0x10) != 0;
				instLen = ebit ? 4 : 3;
			}

			char[] instBytes = rMgr.getMemory(addr, instLen);
			StringBuilder sb = new StringBuilder();
			for (char b : instBytes) {
				sb.append(String.format("%02X", b & 0xFF));
			}
			String code = sb.toString();

			if (code.equalsIgnoreCase("454F46") ||
					code.equalsIgnoreCase("F1") ||
					code.equalsIgnoreCase("001000") ||
					code.equalsIgnoreCase("05") ||
					code.equalsIgnoreCase("05303030")) {
				addr += instLen;
				continue;
			}

			instructionList.add(String.format("%06X  %s", addr, code));
			addr += instLen;
		}
	}

	private boolean isExcludedCode(String code) {
		return code.equalsIgnoreCase("454F46") ||
				code.equalsIgnoreCase("F1") ||
				code.equalsIgnoreCase("001000") ||
				code.equalsIgnoreCase("05");
	}


	private static class ModificationRecord {
		int address;
		int length;
		char sign;
		String symbol;
		int sectionStart;

		ModificationRecord(int address, int length, char sign, String symbol, int sectionStart) {
			this.address = address;
			this.length = length;
			this.sign = sign;
			this.symbol = symbol;
			this.sectionStart = sectionStart;
		}
	}
}
