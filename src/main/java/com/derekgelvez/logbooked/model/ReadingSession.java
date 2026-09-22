package com.derekgelvez.logbooked.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "reading_session", uniqueConstraints = {@UniqueConstraint(columnNames = {"entry_id", "date"})})
@NoArgsConstructor
public class ReadingSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id")
    private Entry entry;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    private Integer currentPage;

    @CreationTimestamp
    private LocalDateTime createdAt;


}
