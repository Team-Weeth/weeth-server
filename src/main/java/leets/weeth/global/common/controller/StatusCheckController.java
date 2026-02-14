package leets.weeth.global.common.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
public class StatusCheckController {

    @GetMapping("/health-check")
    public ResponseEntity<Void> checkHealthStatus() {

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
