import React, { useState } from 'react';
const API_SERVER_HOST = process.env.REACT_APP_API_SERVER_HOST;

const FilePreview = ({ file, isLocal = false }) => {
    const [hasError, setHasError] = useState(false);
    
    // 가공된 displayName이 있으면 사용, 없으면 원본 fileName 사용
    const name = isLocal ? file.name : (file.displayName || file.fileName || "");
    const uuid = isLocal ? null : file.uuid;

    // [수정 포인트] image 속성이 없으므로 확장자 정규식으로 판별
    // 1. 로컬 파일(업로드 전)은 브라우저가 제공하는 file.type으로 판별
    // 2. 서버 파일은 파일명 끝이 이미지 확장자인지 정규식으로 판별
    const isImage = isLocal
        ? file.type?.startsWith('image/')
        : (name && /\.(jpg|jpeg|png|gif|webp|svg)$/i.test(name));

    const imageUrl = isLocal
        ? URL.createObjectURL(file)
        : (uuid ? `${API_SERVER_HOST}/api/files/view/${uuid}` : null);

    // ✅ 이미지 로드 실패 시: state로 관리해서 aspect-square 유지
    if (hasError) {
        return (
            <div className="w-full h-full flex items-center justify-center bg-baseSurface">
                <span className="ui-text-2xs text-baseMuted font-semibold">IMG ERR</span>
            </div>
        );
    }

    if (isImage && imageUrl) {
        return (
            <img
                src={imageUrl}
                alt={name}
                className="w-full h-full object-cover block"
                onError={() => setHasError(true)}
            />
        );
    }

    // 이미지가 아닌 경우 (파일 아이콘 처리)
    const ext = name ? name.split('.').pop().toUpperCase() : 'FILE';
    const colorMap = {
        PDF: 'bg-filePdf',
        ZIP: 'bg-fileZip',
        XLSX: 'bg-fileXlsx',
        DOCX: 'bg-fileDocx',
        TXT: 'bg-fileTxt',
        MP3: 'bg-fileMp3',
        MP4: 'bg-fileMp4',
    };
    const bgClass = colorMap[ext] || 'bg-fileDefault';

    return (
        <div className={`w-full h-full flex flex-col items-center justify-center text-white font-bold ${bgClass}`}>
            <div className="ui-text-4xs opacity-80">FILE</div>
            <div className="text-xs">{ext}</div>
        </div>
    );
};

export default FilePreview;
