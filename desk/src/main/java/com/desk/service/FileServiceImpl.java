package com.desk.service;

import com.desk.domain.TicketFile;
import com.desk.domain.ChatFile;
import com.desk.dto.PageRequestDTO;
import com.desk.dto.PageResponseDTO;
import com.desk.dto.TicketFileDTO;
import com.desk.dto.TicketFilterDTO;
import com.desk.repository.TicketFileRepository;
import com.desk.repository.chat.ChatFileRepository;
import com.desk.util.CustomFileUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileServiceImpl implements FileService {

    private final TicketFileRepository ticketFileRepository;
    private final ChatFileRepository chatFileRepository;
    private final CustomFileUtil fileUtil;

    @Override
    public PageResponseDTO<TicketFileDTO> getFileBoxList(String email, String type, TicketFilterDTO filter, PageRequestDTO pageRequestDTO) {

        // 1. 정렬 및 페이징 설정
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt"); // 기본 최신순
        if ("createdAt,asc".equals(pageRequestDTO.getSort())) {
            sort = Sort.by(Sort.Direction.ASC, "createdAt");
        }

        int size = pageRequestDTO.getSize() > 0 ? pageRequestDTO.getSize() : 10;
        int pageIndex = Math.max(0, pageRequestDTO.getPage() - 1);

        // ✅ 서버에서 ticket_file + chat_file 결과를 합쳐서 "정확한 최신순"으로 페이징하기 위해
        //   (page*size)만큼 먼저 가져와 merge-sort 후 slice 한다.
        //   - 기존 기능(티켓 파일함) 흐름은 유지되고, 채팅 파일이 추가로 섞이는 구조
        int fetchSize = (pageIndex + 1) * size;
        Pageable fetchPageable = PageRequest.of(0, fetchSize, sort);

        // 2. 검색 조건 및 탭 조건에 따른 조회 (QueryDSL 추천이나, 여기선 JPA Method/Spec 예시)
        String kw = (filter.getKeyword() == null) ? "" : filter.getKeyword();

        Page<TicketFile> ticketPage;
        Page<ChatFile> chatPage;

        // 탭 조건: ALL(내가 작성자 OR 수신자), SENT(내가 작성자), RECEIVED(내가 수신자)
        if ("SENT".equals(type)) {
            ticketPage = ticketFileRepository.findByWriterAndSearch(email, kw, fetchPageable);
            chatPage = chatFileRepository.findAccessibleSentByEmailAndSearch(email, kw, fetchPageable);
        } else if ("RECEIVED".equals(type)) {
            ticketPage = ticketFileRepository.findByReceiverAndSearch(email, kw, fetchPageable);
            chatPage = chatFileRepository.findAccessibleReceivedByEmailAndSearch(email, kw, fetchPageable);
        } else {
            ticketPage = ticketFileRepository.findAllByEmailAndSearch(email, kw, fetchPageable);
            chatPage = chatFileRepository.findAccessibleAllByEmailAndSearch(email, kw, fetchPageable);
        }

        // 3. Entity -> DTO 변환 + merge-sort
        List<TicketFileDTO> merged = new ArrayList<>(ticketPage.getNumberOfElements() + chatPage.getNumberOfElements());
        merged.addAll(ticketPage.getContent().stream().map(this::entityToDTO).toList());
        merged.addAll(chatPage.getContent().stream().map(this::chatEntityToDTO).toList());

        Comparator<TicketFileDTO> cmp = Comparator.comparing(TicketFileDTO::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder()));
        if (Sort.Direction.DESC.equals(sort.getOrderFor("createdAt").getDirection())) {
            cmp = cmp.reversed();
        }
        merged.sort(cmp);

        int from = pageIndex * size;
        int to = Math.min(from + size, merged.size());
        List<TicketFileDTO> pageSlice = (from >= merged.size()) ? List.of() : merged.subList(from, to);

        return PageResponseDTO.<TicketFileDTO>withAll()
                .dtoList(pageSlice)
                .pageRequestDTO(pageRequestDTO)
                .totalCount(ticketPage.getTotalElements() + chatPage.getTotalElements())
                .build();
    }
    @Override
    @Transactional
    public void deleteFile(String uuid, String requesterEmail) {
        if (requesterEmail == null || requesterEmail.isBlank()) {
            throw new IllegalArgumentException("요청자 정보가 없습니다.");
        }

        // 1) ticket_file 먼저 확인
        TicketFile ticketFile = ticketFileRepository.findById(uuid).orElse(null);
        if (ticketFile != null) {
            // 권한: writer 또는 receiver(문자열)에 포함된 경우만 허용
            boolean allowed = requesterEmail.equals(ticketFile.getWriter()) ||
                    (ticketFile.getReceiver() != null && ticketFile.getReceiver().contains(requesterEmail));
            if (!allowed) {
                throw new IllegalArgumentException("파일 삭제 권한이 없습니다.");
            }

            fileUtil.deleteFile(ticketFile.getUuid());
            ticketFileRepository.delete(ticketFile);
            return;
        }

        // 2) chat_file 확인
        ChatFile chatFile = chatFileRepository.findById(uuid).orElse(null);
        if (chatFile != null) {
            // 권한: 업로더만 삭제 가능(기존 파일함 삭제 UX와의 충돌 최소화)
            if (!requesterEmail.equals(chatFile.getWriter())) {
                throw new IllegalArgumentException("채팅 파일 삭제 권한이 없습니다.");
            }
            // 참여/입장 이후 권한은 Repository exists로 검증
            if (!chatFileRepository.existsAccessibleChatFileByUuid(uuid, requesterEmail)) {
                throw new IllegalArgumentException("채팅 파일 접근 권한이 없습니다.");
            }
            fileUtil.deleteFile(chatFile.getUuid());
            chatFileRepository.delete(chatFile);
            return;
        }

        throw new IllegalArgumentException("파일을 찾을 수 없습니다.");
    }

    private TicketFileDTO entityToDTO(TicketFile ticketFile) {
        return TicketFileDTO.builder()
                .uuid(ticketFile.getUuid())
                .fileName(ticketFile.getFileName())
                .fileSize(ticketFile.getFileSize())
                .ord(ticketFile.getOrd())
                .createdAt(ticketFile.getCreatedAt())
                .writer(ticketFile.getWriter())
                .receiver(ticketFile.getReceiver())
                .build();
    }

    private TicketFileDTO chatEntityToDTO(ChatFile f) {
        return TicketFileDTO.builder()
                .uuid(f.getUuid())
                .fileName(f.getFileName())
                .fileSize(f.getFileSize())
                .ord(f.getOrd())
                .createdAt(f.getCreatedAt())
                .writer(f.getWriter())
                .receiver(f.getReceiver())
                .build();
    }
}