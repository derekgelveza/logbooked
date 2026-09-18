package com.derekgelvez.logbooked.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "books")
@NoArgsConstructor
public class Book {

    //this is the database ID
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //this is the open library id where we fetch covers and stuff for books
    @Column(nullable = false, unique = true)
    private String openLibraryId;

    private String title;

    private String author;

    private String coverUrl;

    private Integer pageCount;

    private Integer publishedYear;

    private String isbn;

    @CreationTimestamp
    private LocalDateTime createdAt;

}

