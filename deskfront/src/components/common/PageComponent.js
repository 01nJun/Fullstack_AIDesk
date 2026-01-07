const PageComponent = ({ serverData, movePage }) => {
  return (
    <div className="ui-pagination">
      {serverData.prev && (
        <button
          className="ui-pagination-btn-nav"
          onClick={() => movePage({ page: serverData.prevPage })}
        >
          이전
        </button>
      )}

      {serverData.pageNumList.map((pageNum) => (
        <button
          key={pageNum}
          className={serverData.current === pageNum ? "ui-pagination-btn-active w-12" : "ui-pagination-btn w-12"}
          onClick={() => movePage({ page: pageNum })}
        >
          {pageNum}
        </button>
      ))}

      {serverData.next && (
        <button
          className="ui-pagination-btn-nav"
          onClick={() => movePage({ page: serverData.nextPage })}
        >
          다음
        </button>
      )}
    </div>
  );
};

export default PageComponent;
