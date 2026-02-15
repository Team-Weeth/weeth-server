package com.weeth.domain.account.domain.entity;

import jakarta.persistence.*;
import com.weeth.domain.account.application.dto.ReceiptDTO;
import com.weeth.global.common.entity.BaseEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
public class Receipt extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receipt_id")
    private Long id;

    private String description;

    private String source;

    private Integer amount;

    private LocalDate date;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    public void update(ReceiptDTO.Update dto){
        this.description = dto.description();
        this.source = dto.source();
        this.amount = dto.amount();
        this.date = dto.date();
    }

}
