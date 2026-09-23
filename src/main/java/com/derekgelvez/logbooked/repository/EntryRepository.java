package com.derekgelvez.logbooked.repository;

import com.derekgelvez.logbooked.model.Book;
import com.derekgelvez.logbooked.model.Entry;
import com.derekgelvez.logbooked.model.ReadingStatus;
import com.derekgelvez.logbooked.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EntryRepository extends JpaRepository<Entry, Long> {

    List<Entry> findByUser(User user);

    List<Entry> findByUserAndStatus(User user, ReadingStatus status);

    Optional<Entry> findByUserAndBook (User user, Book book);

    Optional<Entry> findByIdAndUser(Long id, User user);

}
