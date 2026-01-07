const FetchingModal = () => {
  return (
    <div className="ui-loading-overlay">
      <div className="ui-loading-panel">
        <div className="text-2xl lg:text-4xl font-semibold text-brandOrange">
          로딩 중...
        </div>
      </div>
    </div>
  );
};

export default FetchingModal;
