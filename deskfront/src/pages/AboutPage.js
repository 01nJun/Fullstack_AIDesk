import { useState } from "react";
import BasicLayout from "../layouts/BasicLayout";
import TicketDetailModal from "../components/ticket/TicketDetailModal";

const AboutPage = () => {
  const [showModal, setShowModal] = useState(false);
  const [tno, setTno] = useState(2);

  const handleOpenModal = () => {
    setShowModal(true);
  };

  const handleCloseModal = () => {
    setShowModal(false);
  };

  return (
    <BasicLayout>
      <div className="p-6">
        <h1 className="text-3xl font-bold mb-6">About - 티켓 모달 테스트</h1>

        <div className="space-y-4">
          <div className="bg-gray-50 p-6 rounded-lg border border-gray-200">
            <h2 className="text-xl font-semibold mb-4">테스트 버튼</h2>
            <div className="space-y-4">
              <div className="flex items-center space-x-4">
                <input
                  type="number"
                  value={tno}
                  onChange={(e) => setTno(Number(e.target.value))}
                  className="border border-gray-300 rounded px-3 py-2 w-32"
                  placeholder="tno 입력"
                  min="1"
                />
                <button
                  onClick={handleOpenModal}
                  className="bg-blue-500 hover:bg-blue-600 text-white px-6 py-3 rounded-lg font-medium"
                >
                  티켓 조회 (tno: {tno})
                </button>
              </div>
            </div>
          </div>
        </div>

        {/* 티켓 모달 (tno 기반 자동 판별) */}
        {showModal && (
          <TicketDetailModal
            tno={tno}
            onClose={handleCloseModal}
            onDelete={handleCloseModal}
          />
        )}

        {/* 설명 섹션 */}
        <div className="mt-8 bg-gray-50 p-6 rounded-lg border border-gray-200">
          <h3 className="text-lg font-semibold mb-4">동작 방식</h3>
          <div className="space-y-2 text-gray-700">
            <p className="font-medium">티켓 상세 조회 프로세스:</p>
            <ol className="list-decimal list-inside space-y-2 ml-2">
              <li>tno를 입력하고 "티켓 조회" 버튼을 클릭합니다.</li>
              <li>
                <strong>자동 판별:</strong> tno로 조회 시, 현재 로그인한 사용자가 티켓의 writer인지 receiver인지 자동으로 판별합니다.
              </li>
              <li>
                <strong>writer인 경우:</strong> 보낸 티켓 모달이 표시됩니다 (삭제 버튼, 수신자 목록 표시).
              </li>
              <li>
                <strong>receiver인 경우:</strong> 받은 티켓 모달이 표시됩니다 (상태 변경 드롭다운 활성화).
              </li>
              <li>
                <strong>둘 다 아닌 경우:</strong> "조회 권한이 없습니다." alert가 표시되고 모달이 열리지 않습니다.
              </li>
            </ol>
            <div className="mt-4 pt-4 border-t border-gray-300">
              <p className="font-medium mb-2">모달 조작:</p>
              <ul className="list-disc list-inside space-y-1 ml-2">
                <li>ESC 키 또는 배경 클릭으로 모달을 닫을 수 있습니다.</li>
                <li>모든 조회는 tno 기준으로 처리되며, 내부적으로 writer/receiver 판별이 이루어집니다.</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </BasicLayout>
  );
};

export default AboutPage;

