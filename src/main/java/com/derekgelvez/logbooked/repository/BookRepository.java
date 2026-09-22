package com.derekgelvez.logbooked.repository;

import com.derekgelvez.logbooked.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByOpenLibraryId(String openLibraryId);
}
