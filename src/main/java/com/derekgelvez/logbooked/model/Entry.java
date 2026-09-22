package com.derekgelvez.logbooked.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.derekgelvez.logbooked.model.ReadingStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "entry")
@NoArgsConstructor
public class Entry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private Book book;

    private Integer rating;

    @Enumerated(EnumType.STRING)
    private ReadingStatus status;

    private String reviewText;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private Integer totalPagesOverride;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;





}
