package com.weeth.domain.account.domain.entity;

import jakarta.persistence.*;
import com.weeth.global.common.entity.BaseEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
public class Account extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long id;

    private String description;

    private Integer totalAmount;

    private Integer currentAmount;

    private Integer cardinal;

    @OneToMany(mappedBy = "account", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Receipt> receipts = new ArrayList<>();

    public void spend(Receipt receipt) {
        this.receipts.add(receipt);
        this.currentAmount -= receipt.getAmount();
    }

    public void cancel(Receipt receipt) {
        this.receipts.remove(receipt);
        this.currentAmount += receipt.getAmount();
    }
}
