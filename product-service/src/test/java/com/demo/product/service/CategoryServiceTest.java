package com.demo.product.service;

import com.demo.product.dto.CategoryDTO;
import com.demo.product.model.Category;
import com.demo.product.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    CategoryRepository categoryRepository;

    @InjectMocks
    CategoryService categoryService;

    @Captor
    ArgumentCaptor<Category> categoryCaptor;

    private CategoryDTO dto;
    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category(1L, "Electronics");
        dto = new CategoryDTO(1L,"Electronics");
    }

    @Test
    void create_shouldReturnSuccess() {
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
        var created = categoryService.create(dto);

        assertEquals("Electronics", created.name());
        verify(categoryRepository).save(categoryCaptor.capture());
        assertEquals("Electronics", categoryCaptor.getValue().getName());
    }

    @Test
    void find_shouldReturnDto() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        var returned = categoryService.find(1L);

        assertEquals("Electronics",returned.name());
        verify(categoryRepository).findById(1L);
        verifyNoMoreInteractions(categoryRepository);
    }

    @Test
    void find_shouldThrow_whenNotFound() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,() ->  categoryService.find(999L));
        verify(categoryRepository).findById(999L);
        verifyNoMoreInteractions(categoryRepository);
    }

    @Test
    void list_shouldReturnPage() {
        var page = new PageImpl<>(List.of(
                new Category(1L, "A"),
                new Category(2L, "B")
        ));

        when(categoryRepository.findAll(any(Pageable.class))).thenReturn(page);

        var output = categoryService.list(PageRequest.of(0, 5 ));

        assertEquals(2, output.getTotalElements());
        assertEquals(page.get().map(Category::getName).toList(), output.getContent().stream().map(CategoryDTO::name).toList());
    }

    @Test
    void update_shouldChangeName() {
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        dto = new CategoryDTO(3L,"NEW");
        var updated = categoryService.update(3L, dto);

        assertEquals("NEW", updated.name());

        verify(categoryRepository).findById(3L);
        verify(categoryRepository).save(category);
        verifyNoMoreInteractions(categoryRepository);
    }

    @Test
    void update_shouldThrow_whenNotFound() {
        when(categoryRepository.findById(3L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, ()-> categoryService.update(3L, dto));

        verify(categoryRepository).findById(3L);
        verifyNoMoreInteractions(categoryRepository);
    }

    @Test
    void delete_shouldThrow_whenNotFound() {
        when(categoryRepository.existsById(3L)).thenReturn(false);
        assertThrows(EntityNotFoundException.class, ()-> categoryService.delete(3L));
        verify(categoryRepository).existsById(3L);
        verifyNoMoreInteractions(categoryRepository);

    }
}