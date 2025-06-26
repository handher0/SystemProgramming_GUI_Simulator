<p align="center">
  <img src="https://github.com/user-attachments/assets/567dad90-29d8-45f0-bb1b-133111a88087" width="600" alt="스크린샷 1"/>
</p>

해당 프로젝트는 Control Section 방식의 SIC/XE Object Program을 실제로 실행하고 시뮬레이션할 수 있는 시각적 도구를 개발하는 것을 목적으로 합니다.

일반적으로 어셈블리 언어로 작성된 프로그램은 어셈블 과정을 거쳐 목적 코드(Object Code)로 변환되며, 이를 실제 시스템에서 실행하려면 로더와 시뮬레이터의 역할이 필요합니다. 

그러나 실제 하드웨어에서 이 과정을 확인하거나 실험하기는 어렵기 때문에, 본 프로젝트에서는 가상의 환경에서 이 과정을 재현하고 그 내부 동작을 시각적으로 확인할 수 있도록 구현하였습니다.

이 시뮬레이터는 사용자가 Object Program 파일을 입력으로 제공하면, 이를 가상 메모리에 적재하고 한 명령어씩 실행하며 레지스터의 변화, 현재 명령어 위치, 명령어 로그 등을 JAVA 기반 GUI를 통해 실시간으로 시각화합니다. 

특히 프로그램이 수행되는 동안의 내부 상태 변화를 직관적으로 관찰할 수 있도록 메모리 상태, 레지스터 값, 수행 중인 명령어를 함께 보여주는 기능을 포함하고 있어, 학습자나 개발자가 SIC/XE 아키텍처의 작동 원리를 깊이 이해할 수 있습니다.


<p align="center">1. 초기 화면</p>
<p align="center">
  <img src="https://github.com/user-attachments/assets/3a656b03-c84d-4bef-967b-08d97f007ae0" width="500" alt="스크린샷 2"/>
</p>

<p align="center">2. 파일 오픈 시 다이얼로그 창</p>
<p align="center">
  <img src="https://github.com/user-attachments/assets/eaa7767d-ee14-48d0-99e0-bf7599c35e36" width="500" alt="스크린샷 3"/>
</p>

<p align="center">3. 로드 결과</p>
<p align="center">
  <img src="https://github.com/user-attachments/assets/d6d0cf9f-530d-4d5a-badf-22da84e4755d" width="500" alt="스크린샷 4"/>
</p>

<p align="center">4. 실행 중</p>
<p align="center">
  <img src="https://github.com/user-attachments/assets/e5f179e4-2f1e-40c0-af29-54c5bee1459f" width="500" alt="스크린샷 5"/>
</p>

<p align="center">5. 실행 완료</p>
<p align="center">
  <img src="https://github.com/user-attachments/assets/6aaf4bc7-78a0-4ef5-ae29-ffc7043c4451" width="500" alt="스크린샷 6"/>
</p>
