package SP25_simulator;

// instruction에 따라 동작을 수행하는 메소드를 정의하는 클래스

import java.util.Arrays;

public class InstLuncher {
    ResourceManager rMgr;
    boolean returnValue;        //다음 명령어에 영향을 미치는 비교 연산의 결과 저장
    String currDevice = "";     //현재 명령어가 사용하고 있는 device의 이름
    String targetAddr = "";     //현재 명령어의 Target Address

    public InstLuncher(ResourceManager resourceManager) {
        this.rMgr = resourceManager;
    }

    // instruction 별로 동작을 수행하는 메소드를 정의
    // ex) public void add(){...}

    public Integer execute(String mnemonic, int nixbpe, int disp, int locctr) {
        switch (mnemonic) {
            case "CLEAR":
                CLEAR(nixbpe, disp);
                return null;
            case "COMP":
                COMP(nixbpe, disp, locctr);
                return null;
            case "COMPR":
                COMPR(nixbpe, disp);
                return null;
            case "J":
                return J(nixbpe, disp, locctr);
            case "JEQ":
                return JEQ(disp, locctr);
            case "JLT":
                return JLT(disp, locctr);
            case "JSUB":
                return JSUB(nixbpe, disp, locctr);
            case "LDA":
                LDA(nixbpe, disp, locctr);
                return null;
            case "LDCH":
                LDCH(nixbpe, disp, locctr);
                return null;
            case "LDT":
                LDT(nixbpe, disp, locctr);
                return null;
            case "RD":
                RD(nixbpe, disp, locctr);
                return null;
            case "RSUB":
                return RSUB();
            case "STL":
                STL(nixbpe, disp, locctr);
                return null;
            case "STX":
                STX(nixbpe, disp, locctr);
                return null;
            case "STA":
                STA(nixbpe, disp, locctr);
                return null;
            case "TD":
                TD(nixbpe, disp, locctr);
                return null;
            case "TIXR":
                TIXR(nixbpe, disp);
                return null;
            case "STCH":
                STCH(nixbpe, disp, locctr);
                return null;
            case "WD":
                WD(nixbpe, disp, locctr);
                return null;
            default:
                return null;
        }
    }

    private int calculateEffectiveAddress(int nixbpe, int disp, int pcAfterInst) {
        int address;

        boolean isPCRelative = (nixbpe & 0x02) != 0; // p
        boolean isBaseRelative = (nixbpe & 0x04) != 0; // b
        boolean isIndexed = (nixbpe & 0x08) != 0; // x

        if (isPCRelative) {
            address = pcAfterInst + disp; // 명령어 다음 주소를 기준으로 disp 적용
        } else if (isBaseRelative) {
            address = rMgr.getRegister(ResourceManager.REG_B) + disp;
        } else {
            address = disp;
        }

        if (isIndexed) {
            address += rMgr.getRegister(ResourceManager.REG_X);
        }

        return address;
    }

    private void LDA(int nixbpe, int disp, int locctr) {
        boolean isImmediate = (nixbpe & 0x30) == 0x10;

        if (isImmediate) {
            rMgr.setRegister(ResourceManager.REG_A, disp); // immediate value
            System.out.println("LDA (immediate) → A set to: " + disp);
        } else {
            int address = calculateEffectiveAddress(nixbpe, disp, locctr);
            char[] memData = rMgr.getMemory(address, 3); // 3바이트
            int value = rMgr.byteToInt(memData);
            rMgr.setRegister(ResourceManager.REG_A, value); // A 레지스터에 로드
            System.out.println("LDA → effective address: " + String.format("%04X", address));
            System.out.println("LDA → value loaded: " + value);
        }
    }

    private void STL(int nixbpe, int disp, int locctr) {
        int address = calculateEffectiveAddress(nixbpe, disp, locctr);
        int value = rMgr.getRegister(ResourceManager.REG_L); // L 레지스터 값 읽기
        char[] bytes = rMgr.intToChar(value, 3); // 3바이트로 변환
        rMgr.setMemory(address, bytes, 3); // 메모리에 저장

        System.out.println("STL → effective address: " + String.format("%06X", address));
        System.out.println("STL → value stored: " + String.format("%06X", value));
    }

    private void STX(int nixbpe, int disp, int locctr) {
        int address = calculateEffectiveAddress(nixbpe, disp, locctr);
        int value = rMgr.getRegister(ResourceManager.REG_X); // X 레지스터 값 읽기
        char[] bytes = rMgr.intToChar(value, 3); // 3바이트로 변환
        rMgr.setMemory(address, bytes, 3); // 메모리에 저장
    }

    private void STA(int nixbpe, int disp, int locctr) {
        int address = calculateEffectiveAddress(nixbpe, disp, locctr);
        int value = rMgr.getRegister(ResourceManager.REG_A); // A 레지스터 값 읽기
        char[] bytes = rMgr.intToChar(value, 3); // 3바이트로 변환
        rMgr.setMemory(address, bytes, 3); // 메모리에 저장
    }

    private void CLEAR(int nixbpe, int disp) {
        int r = (disp >> 4) & 0x0F; // 상위 4비트 추출
        rMgr.setRegister(r, 0); // 해당 레지스터 값을 0으로 설정
    }

    private void LDT(int nixbpe, int disp, int locctr) {
        int address = calculateEffectiveAddress(nixbpe, disp, locctr);
        char[] memData = rMgr.getMemory(address, 3); // 3바이트
        int value = rMgr.byteToInt(memData);
        rMgr.setRegister(ResourceManager.REG_T, value); // T 레지스터에 로드
    }

    private void TD(int nixbpe, int disp, int locctr) {
        int address = calculateEffectiveAddress(nixbpe, disp, locctr);
        char[] memData = rMgr.getMemory(address, 1);
        String deviceIdForTest = String.valueOf(memData[0]);  // ASCII 문자로 비교용
        String deviceIdForDisplay = String.format("%02X", (int) memData[0]);  // GUI용 F1 표기
        currDevice = deviceIdForDisplay;
        boolean available = rMgr.testDevice(deviceIdForTest);
        rMgr.setRegister(ResourceManager.REG_SW, available ? 0 : 1);
    }

    private void RD(int nixbpe, int disp, int locctr) {
        int address = calculateEffectiveAddress(nixbpe, disp, locctr);
        System.out.println("RD() → effective address: " + String.format("%04X", address));

        char[] memData = rMgr.getMemory(address, 1);
        StringBuilder sb = new StringBuilder();
        for (char c : memData) {
            sb.append(String.format("%02X", (int) c));
        }
        String deviceId = sb.toString();
        currDevice = deviceId;

        char[] readChars = rMgr.readDevice(deviceId); // read hex representation
        int readValue = 0;

        if (readChars.length >= 2) {
            String hexStr = "" + readChars[0] + readChars[1];
            try {
                readValue = Integer.parseInt(hexStr, 16);
            } catch (NumberFormatException e) {
                readValue = 0;
            }
            System.out.println("RD() → read char: " + new String(readChars));
            System.out.println("RD() → ASCII value stored to A: " + readValue);
        } else {
            System.out.println("RD() → no valid data read, storing 0");
        }

        rMgr.setRegister(ResourceManager.REG_A, readValue);

        System.out.println("RD() → raw memData: " + Arrays.toString(memData));
        System.out.println("RD() → deviceId (hex): " + deviceId);
    }

    private Integer JEQ(int disp, int locctr) {
        int cc = rMgr.getRegister(ResourceManager.REG_SW);
        if (cc == 0) {
            return locctr + disp;
        }
        return null;
    }

    private void COMPR(int nixbpe, int disp) {
        // Format 2: disp = 1 byte where upper 4 bits = r1, lower 4 bits = r2
        int r1 = (disp >> 4) & 0x0F;
        int r2 = disp & 0x0F;

        System.out.println("COMPR → r1: " + r1 + ", r2: " + r2);

        int val1 = rMgr.getRegister(r1);
        int val2 = rMgr.getRegister(r2);

        System.out.println("COMPR → val1: " + val1 + ", val2: " + val2);

        int conditionCode;
        if (val1 < val2) {
            conditionCode = -1; // LT
        } else if (val1 == val2) {
            conditionCode = 0;  // EQ
        } else {
            conditionCode = 1;  // GT
        }

        System.out.println("COMPR → conditionCode set to: " + conditionCode);
        rMgr.setRegister(ResourceManager.REG_SW, conditionCode); // 결과를 SW에 저장
    }

    private void COMP(int nixbpe, int disp, int locctr) {
        int aVal = rMgr.getRegister(ResourceManager.REG_A); // A 레지스터 값
        int operand;

        boolean isImmediate = (nixbpe & 0x30) == 0x10;

        if (isImmediate) {
            operand = disp; // immediate 모드는 disp 그대로 사용
        } else {
            int address = calculateEffectiveAddress(nixbpe, disp, locctr);
            char[] memData = rMgr.getMemory(address, 3);
            operand = rMgr.byteToInt(memData);
        }

        int conditionCode;
        if (aVal < operand) {
            conditionCode = -1;
        } else if (aVal == operand) {
            conditionCode = 0;
        } else {
            conditionCode = 1;
        }

        rMgr.setRegister(ResourceManager.REG_SW, conditionCode);
        System.out.printf("COMP → A: %d, operand: %d, cc: %d\n", aVal, operand, conditionCode);
    }

    private void LDCH(int nixbpe, int disp, int locctr) {
        int address = calculateEffectiveAddress(nixbpe, disp, locctr);
        char[] memData = rMgr.getMemory(address, 1); // LDCH는 1바이트만 읽음
        int value = memData[0] & 0xFF; // char를 unsigned byte로 처리
        rMgr.setRegister(ResourceManager.REG_A, value); // A 레지스터에 저장

        System.out.println("LDCH → effective address: " + String.format("%04X", address));
        System.out.println("LDCH → value loaded: " + value);
    }

    private Integer JSUB(int nixbpe, int disp, int locctr) {
        int address = calculateEffectiveAddress(nixbpe, disp, locctr);

        rMgr.setRegister(ResourceManager.REG_L, locctr); // Return address (next instruction)
        return address;
    }

    private void TIXR(int nixbpe, int disp) {
        int r = (disp >> 4) & 0x0F; // 실제로는 byte2 전체가 들어오므로 상위 4비트만 사용

        // 1. X 레지스터 증가
        int xVal = rMgr.getRegister(ResourceManager.REG_X);
        xVal++;
        rMgr.setRegister(ResourceManager.REG_X, xVal);

        // 2. 비교 대상 레지스터 값 가져오기
        int rVal = rMgr.getRegister(r);

        // 3. 조건 코드 계산
        int conditionCode;
        if (xVal < rVal) {
            conditionCode = -1; // LT
        } else if (xVal == rVal) {
            conditionCode = 0;  // EQ
        } else {
            conditionCode = 1;  // GT
        }

        rMgr.setRegister(ResourceManager.REG_SW, conditionCode);
    }

    private Integer JLT(int disp, int locctr) {
        int cc = rMgr.getRegister(ResourceManager.REG_SW);
        if (cc < 0) { // condition code -1: LT
            return locctr + disp;
        }
        return null;
    }

    private Integer RSUB() {
        currDevice = null;
        return rMgr.getRegister(ResourceManager.REG_L); // L 레지스터의 값을 PC로 설정
    }

    private Integer J(int nixbpe, int disp, int locctr) {
        // Extract n and i flags separately for clarity
        boolean nFlag = (nixbpe & 0x20) != 0;
        boolean iFlag = (nixbpe & 0x10) != 0;
        boolean isIndirect = nFlag && !iFlag;

        // sign-extend 12-bit displacement if necessary
        if ((disp & 0x800) != 0) {
            disp |= 0xFFFFF000;
        }

        int target = locctr + disp;

        if (isIndirect) {
            char[] mem = rMgr.getMemory(target, 3); // 3바이트 메모리에서 주소 추출
            target = rMgr.byteToInt(mem);
            System.out.println("J (indirect) → jump to address in memory: " + String.format("%06X", target));

            if (target == 0x000000 || target == 0xFFFFFF) {
                System.out.println("J (indirect) → terminating program due to target = " + String.format("%06X", target));
                rMgr.setRegister(ResourceManager.REG_PC, target); // 강제로 PC 설정
                }
        }
        else {
            System.out.println("J (direct) → jump to address: " + String.format("%06X", target));
        }
        System.out.printf("J → format4 target: %06X (isIndirect: %b)%n", target, isIndirect);

        return target;
    }

    private void STCH(int nixbpe, int disp, int locctr) {
        int address = calculateEffectiveAddress(nixbpe, disp, locctr);
        int aVal = rMgr.getRegister(ResourceManager.REG_A);
        char[] data = new char[1];
        data[0] = (char) (aVal & 0xFF); // only lowest byte stored
        rMgr.setMemory(address, data, 1); // store 1 byte in memory

        System.out.println("STCH → effective address: " + String.format("%04X", address));
        System.out.println("STCH → value stored: " + String.format("%02X", (int)data[0]));
    }

    private void WD(int nixbpe, int disp, int locctr) {
        int address = calculateEffectiveAddress(nixbpe, disp, locctr);
        char[] memData = rMgr.getMemory(address, 1); // 1바이트: 디바이스 ID
        StringBuilder sb = new StringBuilder();
        for (char c : memData) {
            sb.append(String.format("%02X", (int) c));
        }
        String deviceId = sb.toString();
        currDevice = deviceId;

        int aVal = rMgr.getRegister(ResourceManager.REG_A);
        char data = (char) (aVal & 0xFF); // A 레지스터의 하위 1바이트
        rMgr.writeDevice(deviceId, data);

        System.out.println("WD() → effective address: " + String.format("%04X", address));
        System.out.println("WD() → deviceId (hex): " + deviceId);
        System.out.println("WD() → data written: " + String.format("%02X", (int)data));
    }
}