package com.desk.controller;

import com.desk.dto.TicketPinResponseDTO;
import com.desk.service.TicketPinService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/tickets/pins")
@RequiredArgsConstructor
public class TicketPinController {
    private final TicketPinService pinService;

    @GetMapping("/items")
    public List<TicketPinResponseDTO> getItems(Principal principal) {
        // Principal에서 이메일 추출 (보안 설정에 맞춰 수정 가능)
        return pinService.getPinList(principal.getName());
    }

    @PostMapping("/toggle/{tno}")
    public List<TicketPinResponseDTO> toggle(@PathVariable("tno") Long tno, Principal principal) {
        return pinService.togglePin(principal.getName(), tno);
    }
}