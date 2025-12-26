 package com.desk.service;

 import com.desk.domain.Board;
 import com.desk.domain.Reply;
 import com.desk.dto.PageRequestDTO;
 import com.desk.dto.PageResponseDTO;
 import com.desk.dto.ReplyDTO;
 import com.desk.repository.ReplyRepository;
 import lombok.RequiredArgsConstructor;
 import lombok.extern.log4j.Log4j2;
 import org.modelmapper.ModelMapper;
 import org.springframework.data.domain.Page;
 import org.springframework.data.domain.PageRequest;
 import org.springframework.data.domain.Pageable;
 import org.springframework.data.domain.Sort;
 import org.springframework.stereotype.Service;
 import org.springframework.transaction.annotation.Transactional;

 import java.util.List;
 import java.util.stream.Collectors;

 @Service // [필수] 이 어노테이션이 있어야 서버가 정상 실행됩니다.
 @RequiredArgsConstructor
 @Log4j2
 @Transactional
 public class ReplyServiceImpl implements ReplyService {

     private final ReplyRepository replyRepository;
     private final ModelMapper modelMapper;

     @Override
     public Long register(ReplyDTO replyDTO) {
         // DTO를 엔티티로 변환
         // 만약 DTO에 bno가 있다면 Board 객체를 맵핑해주어야 합니다.
         Reply reply = modelMapper.map(replyDTO, Reply.class);
        
         // 연관관계가 필요하다면 아래와 같이 처리할 수 있습니다.
         // Board board = Board.builder().bno(replyDTO.getBno()).build();
         // reply.setBoard(board);

         log.info("댓글 등록: " + reply);
         return replyRepository.save(reply).getRno();
     }

     @Override
     public ReplyDTO read(Long rno) {
         return replyRepository.findById(rno)
                 .map(reply -> modelMapper.map(reply, ReplyDTO.class))
                 .orElseThrow();
     }

     @Override
     public void modify(ReplyDTO replyDTO) {
         Reply reply = replyRepository.findById(replyDTO.getRno()).orElseThrow();
         // [주의] 이미지 상에서 빨간줄이 떴던 부분: Reply 엔티티에 changeText가 있어야 합니다.
         reply.changeText(replyDTO.getReplyText());
         replyRepository.save(reply);
     }

     @Override
     public void remove(Long rno) {
         replyRepository.deleteById(rno);
     }

     @Override
     public PageResponseDTO<ReplyDTO> getListOfBoard(Long bno, PageRequestDTO pageRequestDTO) {
        
         Pageable pageable = PageRequest.of(
                 pageRequestDTO.getPage() <= 0 ? 0 : pageRequestDTO.getPage() - 1,
                 pageRequestDTO.getSize(),
                 Sort.by("rno").ascending());

         Page<Reply> result = replyRepository.listOfBoard(bno, pageable);

         List<ReplyDTO> dtoList = result.getContent().stream()
                 .map(reply -> modelMapper.map(reply, ReplyDTO.class))
                 .collect(Collectors.toList());

         return PageResponseDTO.<ReplyDTO>withAll()
                 .pageRequestDTO(pageRequestDTO)
                 .dtoList(dtoList)
                 .totalCount((int)result.getTotalElements())
                 .build();
     }
 }