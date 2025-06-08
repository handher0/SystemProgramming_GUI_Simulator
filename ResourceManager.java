package SP25_simulator;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Map;
import java.io.FileInputStream;

/**
 * ResourceManager는 컴퓨터의 가상 리소스들을 선언하고 관리하는 클래스이다. 크게 네가지의 가상 자원 공간을 선언하고, 이를
 * 관리할 수 있는 함수들을 제공한다.
 *
 *
 * 1) 입출력을 위한 외부 장치 또는 device 2) 프로그램 로드 및 실행을 위한 메모리 공간. 여기서는 64KB를 최대값으로 잡는다.
 * 3) 연산을 수행하는데 사용하는 레지스터 공간. 4) SYMTAB 등 simulator의 실행 과정에서 사용되는 데이터들을 위한 변수들.
 *
 * 2번은 simulator위에서 실행되는 프로그램을 위한 메모리공간인 반면, 4번은 simulator의 실행을 위한 메모리 공간이라는 점에서
 * 차이가 있다.
 */
public class ResourceManager {
	/**
	 * 디바이스는 원래 입출력 장치들을 의미 하지만 여기서는 파일로 디바이스를 대체한다. 즉, 'F1'이라는 디바이스는 'F1'이라는 이름의
	 * 파일을 의미한다. deviceManager는 디바이스의 이름을 입력받았을 때 해당 이름의 파일 입출력 관리 클래스를 리턴하는 역할을 한다.
	 * 예를 들어, 'A1'이라는 디바이스에서 파일을 read모드로 열었을 경우, hashMap에 <"A1", scanner(A1)> 등을
	 * 넣음으로서 이를 관리할 수 있다.
	 *
	 * 변형된 형태로 사용하는 것 역시 허용한다. 예를 들면 key값으로 String대신 Integer를 사용할 수 있다. 파일 입출력을 위해
	 * 사용하는 stream 역시 자유로이 선택, 구현한다.
	 *
	 * 이것도 복잡하면 알아서 구현해서 사용해도 괜찮습니다.
	 */
	HashMap<String, FileInputStream> deviceReaderMap = new HashMap<>();
	HashMap<String, FileWriter> deviceWriterMap = new HashMap<>();
	Map<String, Integer> deviceReadPositions = new HashMap<>();
	ArrayList<String> readerList = new ArrayList<>();
	ArrayList<String> writerList = new ArrayList<>();
	char[] memory = new char[65536]; // String으로 수정해서 사용하여도 무방함.
	int[] register = new int[10];
	double REG_F;

    // 레지스터 이름으로 접근할 수 있도록
    public static final int REG_A = 0; // Accumulator
    public static final int REG_X = 1; // Index register
    public static final int REG_L = 2; // Linkage register
    public static final int REG_B = 3; // Base register
    public static final int REG_S = 4; // General register
    public static final int REG_T = 5; // General register
    //F는 더블이니까 따로
    public static final int REG_PC = 8; // Program counter
    public static final int REG_SW = 9; // Status word

	SymbolTable symtab;
	// 이외에도 필요한 변수 선언해서 사용할 것.
	public ResourceManager() {
		symtab = new SymbolTable();
	}

	/**
	 * 메모리, 레지스터등 가상 리소스들을 초기화한다.
	 */
	public void initializeResource() throws IOException {
		Arrays.fill(memory, '0');
		Arrays.fill(register, 0);
		REG_F = 0;
		register[REG_L] = 0xFFFFFF;
		this.symtab = new SymbolTable();
		closeDevice();
	}

	/**
	 * deviceManager가 관리하고 있는 파일 입출력 stream들을 전부 종료시키는 역할. 프로그램을 종료하거나 연결을 끊을 때
	 * 호출한다.
	 */
	public void closeDevice() throws IOException {
		for (String readDiv : readerList) {
			FileInputStream reader = deviceReaderMap.get(readDiv);
			if (reader != null) reader.close();
		}
		for (String writeDiv : writerList) {
			FileWriter writer = deviceWriterMap.get(writeDiv);
			if (writer != null) writer.close();
		}
		readerList.clear();
		writerList.clear();
		deviceReaderMap.clear();
		deviceWriterMap.clear();
		deviceReadPositions.clear();
	}
	/**
	 * 디바이스를 사용할 수 있는 상황인지 체크. TD명령어를 사용했을 때 호출되는 함수. 입출력 stream을 열고 deviceManager를
	 * 통해 관리시킨다.
	 *
	 * @param devName 확인하고자 하는 디바이스의 번호,또는 이름
	 */
	public boolean testDevice(String devName) {
		File device = new File(devName + ".device");
		return device.exists() && device.canWrite() && device.canRead();
	}
	/**
	 * 디바이스로부터 원하는 개수만큼의 글자를 읽어들인다. RD명령어를 사용했을 때 호출되는 함수.
	 *
	 * @param devName 디바이스의 이름
	 * @return 가져온 데이터
	 */
	public char[] readDevice(String devName) {
		try {
			FileInputStream inputStream;
			int readPosition = deviceReadPositions.getOrDefault(devName, 0);

			if (!deviceReaderMap.containsKey(devName)) {
				File rDevice = new File(devName + ".device");
				inputStream = new FileInputStream(rDevice);
				deviceReaderMap.put(devName, inputStream);
				deviceReadPositions.put(devName, 0);
				readerList.add(devName);
			} else {
				inputStream = deviceReaderMap.get(devName);
			}

			// 스트림에서 1바이트 읽기
			int data = inputStream.read();
			if (data == -1) {
				return new char[]{'0', '0'}; // 더 이상 읽을 게 없으면 '0', '0' 반환
			}

			// 읽은 위치 갱신
			deviceReadPositions.put(devName, readPosition + 1);

			// 1바이트를 2자리 16진수 문자로 변환
			char high = Character.toUpperCase(Character.forDigit((data >> 4) & 0xF, 16));
			char low = Character.toUpperCase(Character.forDigit(data & 0xF, 16));
			return new char[]{high, low};

		} catch (IOException e) {
			e.printStackTrace();
			return new char[]{'0', '0'};
		}
	}

	/**
	 * 디바이스로 원하는 개수 만큼의 글자를 출력한다. WD명령어를 사용했을 때 호출되는 함수.
	 *
	 * @param devName 디바이스의 이름
	 * @param data    보내는 데이터
	 */
	public void writeDevice(String devName, char data) {
		try {
			if (!deviceWriterMap.containsKey(devName)) {
				File wDevice = new File(devName + ".device");
				FileWriter fw = new FileWriter(wDevice);
				deviceWriterMap.put(devName, fw);
				writerList.add(devName);
			}
			FileWriter writer = deviceWriterMap.get(devName);
			writer.write(data);
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 메모리의 특정 위치에서 원하는 개수만큼의 글자를 가져온다.
	 *
	 * @param location 메모리 접근 위치 인덱스
	 * @param num      데이터 개수
	 * @return 가져오는 데이터
	 */
	public char[] getMemory(int location, int num) {
		char[] result = new char[num];
		System.arraycopy(memory, location, result, 0, num);
		return result;
	}

	/**
	 * 메모리의 특정 위치에 원하는 개수만큼의 데이터를 저장한다.
	 *
	 * @param locate 접근 위치 인덱스
	 * @param data   저장하려는 데이터
	 * @param num    저장하는 데이터의 개수
	 */
	public void setMemory(int locate, char[] data, int num) {
		System.arraycopy(data, 0, memory, locate, num);
	}

	/**
	 * 번호에 해당하는 레지스터가 현재 들고 있는 값을 리턴한다. 레지스터가 들고 있는 값은 문자열이 아님에 주의한다.
	 *
	 * @param regNum 레지스터 분류번호
	 * @return 레지스터가 소지한 값
	 */
	public int getRegister(int regNum) {
		if (regNum >= 0 && regNum < register.length) return register[regNum];
		return 0;
	}

	/**
	 * 번호에 해당하는 레지스터에 새로운 값을 입력한다. 레지스터가 들고 있는 값은 문자열이 아님에 주의한다.
	 *
	 * @param regNum 레지스터의 분류번호
	 * @param value  레지스터에 집어넣는 값
	 */
	public void setRegister(int regNum, int value) {
		if (regNum >= 0 && regNum < register.length) register[regNum] = value;
	}

	/**
	 * 주로 레지스터와 메모리간의 데이터 교환에서 사용된다. int값을 char[]형태로 변경한다.
	 *
	 * @param data
	 * @param length
	 * @return
	 */
	public char[] intToChar(int data, int length) {
	    char[] result = new char[length];
	    for (int i = length - 1; i >= 0; i--) {
	        result[i] = (char) (data & 0xFF);
	        data >>= 8;
	    }
	    return result;
	}

	/**
	 * 주로 레지스터와 메모리간의 데이터 교환에서 사용된다. char[]값을 int형태로 변경한다.
	 *
	 * @param data
	 * @return
	 */
	public int byteToInt(char[] data) {
		int result = 0;
		for (int i = 0; i < data.length; i++) {
			result = (result << 8) | (data[i] & 0xFF);
		}
		return result;
	}
    /**
     * 조건 코드 값을 설정한다.
     * 조건 코드는 COMP, COMPR 등의 비교 명령어 결과에 따라 설정되며,
     * 분기 명령(JEQ, JGT, JLT 등)의 수행 여부를 결정하는 데 사용된다.
     *
     * @param code 설정할 조건 코드 ("EQ", "LT", "GT" 중 하나)
     */
    private String conditionCode;

    public void setConditionCode(String code) {
        if (code.equals("EQ") || code.equals("LT") || code.equals("GT")) {
            this.conditionCode = code;
        } else {
            throw new IllegalArgumentException("Invalid condition code: " + code);
        }
    }

    /**
     * 조건 코드 값을 반환한다.
     *
     * @return 현재 설정된 조건 코드
     */
    public String getConditionCode() {
        return conditionCode;
    }

	/**
	 * 현재 메모리 상태를 16진수 형식으로 출력한다.
	 * 디버깅 용도로 사용한다.
	 */
	public void printMemoryDump(int start, int end) {
		for (int i = start; i < end; i += 16) {
			System.out.printf("%04X : ", i);
			for (int j = 0; j < 16 && i + j < memory.length; j++) {
				System.out.printf("%02X ", (int) memory[i + j] & 0xFF);
			}
			System.out.println();
		}
	}
}
