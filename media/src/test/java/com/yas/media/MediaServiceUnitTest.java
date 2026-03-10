package com.yas.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.media.config.YasConfig;
import com.yas.media.mapper.MediaVmMapper;
import com.yas.media.model.Media;
import com.yas.media.model.dto.MediaDto;
import com.yas.media.model.dto.MediaDto.MediaDtoBuilder;
import com.yas.media.repository.FileSystemRepository;
import com.yas.media.repository.MediaRepository;
import com.yas.media.service.MediaServiceImpl;
import com.yas.media.viewmodel.MediaPostVm;
import com.yas.media.viewmodel.MediaVm;
import com.yas.media.viewmodel.NoFileMediaVm;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class MediaServiceUnitTest {

    @Spy
    private MediaVmMapper mediaVmMapper = Mappers.getMapper(MediaVmMapper.class);

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private FileSystemRepository fileSystemRepository;

    @Mock
    private YasConfig yasConfig;

    @Mock
    private MediaDtoBuilder builder;

    @InjectMocks
    private MediaServiceImpl mediaService;

    private Media media;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        media = new Media();
        media.setId(1L);
        media.setCaption("test");
        media.setFileName("file");
        media.setMediaType("image/jpeg");
    }

    @Test
    void getMedia_whenValidId_thenReturnData() {
        NoFileMediaVm noFileMediaVm = new NoFileMediaVm(1L, "Test", "fileName", "image/png");
        when(mediaRepository.findByIdWithoutFileInReturn(1L)).thenReturn(noFileMediaVm);
        when(yasConfig.publicUrl()).thenReturn("/media/");

        MediaVm mediaVm = mediaService.getMediaById(1L);
        assertNotNull(mediaVm);
        assertEquals("Test", mediaVm.getCaption());
        assertEquals("fileName", mediaVm.getFileName());
        assertEquals("image/png", mediaVm.getMediaType());
        assertEquals(String.format("/media/medias/%s/file/%s", 1L, "fileName"), mediaVm.getUrl());
    }

    @Test
    void getMedia_whenMediaNotFound_thenReturnNull() {
        when(mediaRepository.findById(1L)).thenReturn(Optional.empty());

        MediaVm mediaVm = mediaService.getMediaById(1L);
        assertNull(mediaVm);
    }

    @Test
    void removeMedia_whenMediaNotFound_thenThrowsNotFoundException() {
        when(mediaRepository.findById(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> mediaService.removeMedia(1L));
        assertEquals(String.format("Media %s is not found", 1L), exception.getMessage());
    }

    @Test
    void removeMedia_whenValidId_thenRemoveSuccess() {
        NoFileMediaVm noFileMediaVm = new NoFileMediaVm(1L, "Test", "fileName", "image/png");
        when(mediaRepository.findByIdWithoutFileInReturn(1L)).thenReturn(noFileMediaVm);
        doNothing().when(mediaRepository).deleteById(1L);

        mediaService.removeMedia(1L);

        verify(mediaRepository, times(1)).deleteById(1L);
    }

    @Test
    void saveMedia_whenTypePNG_thenSaveSuccess() {
        byte[] pngFileContent = new byte[] {};
        MultipartFile multipartFile = new MockMultipartFile(
            "file",
            "example.png",
            "image/png",
            pngFileContent
        );
        MediaPostVm mediaPostVm = new MediaPostVm("media", multipartFile, "fileName");

        when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Media mediaSave = mediaService.saveMedia(mediaPostVm);
        assertNotNull(mediaSave);
        assertEquals("media", mediaSave.getCaption());
        assertEquals("fileName", mediaSave.getFileName());
    }

    @Test
    void saveMedia_whenTypeJPEG_thenSaveSuccess() {
        byte[] pngFileContent = new byte[] {};
        MultipartFile multipartFile = new MockMultipartFile(
            "file",
            "example.jpeg",
            "image/jpeg",
            pngFileContent
        );
        MediaPostVm mediaPostVm = new MediaPostVm("media", multipartFile, "fileName");

        when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Media mediaSave = mediaService.saveMedia(mediaPostVm);
        assertNotNull(mediaSave);
        assertEquals("media", mediaSave.getCaption());
        assertEquals("fileName", mediaSave.getFileName());
    }

    @Test
    void saveMedia_whenTypeGIF_thenSaveSuccess() {
        byte[] gifFileContent = new byte[] {};
        MultipartFile multipartFile = new MockMultipartFile(
            "file",
            "example.gif",
            "image/gif",
            gifFileContent
        );
        MediaPostVm mediaPostVm = new MediaPostVm("media", multipartFile, "fileName");

        when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Media mediaSave = mediaService.saveMedia(mediaPostVm);
        assertNotNull(mediaSave);
        assertEquals("media", mediaSave.getCaption());
        assertEquals("fileName", mediaSave.getFileName());
    }

    @Test
    void saveMedia_whenFileNameIsNull_thenOk() {
        byte[] pngFileContent = new byte[] {};
        MultipartFile multipartFile = new MockMultipartFile(
            "file",
            "example.png",
            "image/png",
            pngFileContent
        );
        MediaPostVm mediaPostVm = new MediaPostVm("media", multipartFile, null);

        when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Media mediaSave = mediaService.saveMedia(mediaPostVm);
        assertNotNull(mediaSave);
        assertEquals("media", mediaSave.getCaption());
        assertEquals("example.png", mediaSave.getFileName());
    }

    @Test
    void saveMedia_whenFileNameIsEmpty_thenOk() {
        byte[] pngFileContent = new byte[] {};
        MultipartFile multipartFile = new MockMultipartFile(
            "file",
            "example.png",
            "image/png",
            pngFileContent
        );
        MediaPostVm mediaPostVm = new MediaPostVm("media", multipartFile, "");

        when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Media mediaSave = mediaService.saveMedia(mediaPostVm);
        assertNotNull(mediaSave);
        assertEquals("media", mediaSave.getCaption());
        assertEquals("example.png", mediaSave.getFileName());
    }

    @Test
    void saveMedia_whenFileNameIsBlank_thenOk() {
        byte[] pngFileContent = new byte[] {};
        MultipartFile multipartFile = new MockMultipartFile(
            "file",
            "example.png",
            "image/png",
            pngFileContent
        );
        MediaPostVm mediaPostVm = new MediaPostVm("media", multipartFile, "   ");

        when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Media mediaSave = mediaService.saveMedia(mediaPostVm);
        assertNotNull(mediaSave);
        assertEquals("media", mediaSave.getCaption());
        assertEquals("example.png", mediaSave.getFileName());
    }

    @Test
    void getFile_whenMediaNotFound_thenReturnMediaDto() {
        MediaDto expectedDto = MediaDto.builder().build();
        when(mediaRepository.findById(1L)).thenReturn(Optional.ofNullable(null));
        when(builder.build()).thenReturn(expectedDto);

        MediaDto mediaDto = mediaService.getFile(1L, "fileName");

        assertEquals(expectedDto.getMediaType(), mediaDto.getMediaType());
        assertEquals(expectedDto.getContent(), mediaDto.getContent());
    }

    @Test
    void getFile_whenMediaNameNotMatch_thenReturnMediaDto() {
        MediaDto expectedDto = MediaDto.builder().build();
        when(mediaRepository.findById(1L)).thenReturn(Optional.ofNullable(media));
        when(builder.build()).thenReturn(expectedDto);

        MediaDto mediaDto = mediaService.getFile(1L, "fileName");

        assertEquals(expectedDto.getMediaType(), mediaDto.getMediaType());
        assertEquals(expectedDto.getContent(), mediaDto.getContent());
    }

    @Test
    void getFileByIds() {
        // Given
        var ip15 = getMedia(-1L, "Iphone 15");
        var macbook = getMedia(-2L, "Macbook");
        var existingMedias = List.of(ip15, macbook);
        when(mediaRepository.findAllById(List.of(ip15.getId(), macbook.getId())))
            .thenReturn(existingMedias);
        when(yasConfig.publicUrl()).thenReturn("https://media/");

        // When
        var medias = mediaService.getMediaByIds(List.of(ip15.getId(), macbook.getId()));

        // Then
        assertFalse(medias.isEmpty());
        verify(mediaVmMapper, times(existingMedias.size())).toVm(any());
        assertThat(medias).allMatch(m -> m.getUrl() != null);
    }

    @Test
    void saveMedia_verifyFileSystemPersistenceAndFilePath() throws Exception {
        byte[] fileContent = new byte[] {1, 2, 3};
        MultipartFile multipartFile = new MockMultipartFile(
            "file", "example.png", "image/png", fileContent
        );
        MediaPostVm mediaPostVm = new MediaPostVm("caption", multipartFile, "custom.png");

        when(fileSystemRepository.persistFile(any(String.class), any(byte[].class)))
            .thenReturn("/storage/custom.png");
        when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Media result = mediaService.saveMedia(mediaPostVm);

        assertNotNull(result);
        assertEquals("/storage/custom.png", result.getFilePath());
        verify(fileSystemRepository, times(1)).persistFile(any(String.class), any(byte[].class));
    }

    @Test
    void saveMedia_whenFileNameOverrideHasLeadingTrailingSpaces_thenTrimmed() {
        MultipartFile multipartFile = new MockMultipartFile(
            "file", "example.png", "image/png", new byte[] {}
        );
        MediaPostVm mediaPostVm = new MediaPostVm("caption", multipartFile, "  trimmed.png  ");

        when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Media result = mediaService.saveMedia(mediaPostVm);

        assertEquals("trimmed.png", result.getFileName());
    }

    @Test
    void saveMedia_verifyMediaTypeFromMultipartFile() {
        MultipartFile multipartFile = new MockMultipartFile(
            "file", "photo.jpg", "image/jpeg", new byte[] {}
        );
        MediaPostVm mediaPostVm = new MediaPostVm("photo", multipartFile, "photo.jpg");

        when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Media result = mediaService.saveMedia(mediaPostVm);

        assertEquals("image/jpeg", result.getMediaType());
    }

    @Test
    void getFile_whenMediaFoundAndNameMatches_thenReturnContentAndMediaType() {
        Media matchingMedia = new Media();
        matchingMedia.setId(1L);
        matchingMedia.setFileName("photo.png");
        matchingMedia.setFilePath("/storage/photo.png");
        matchingMedia.setMediaType("image/png");

        InputStream mockContent = new ByteArrayInputStream(new byte[] {1, 2, 3});

        when(mediaRepository.findById(1L)).thenReturn(Optional.of(matchingMedia));
        when(fileSystemRepository.getFile("/storage/photo.png")).thenReturn(mockContent);

        MediaDto result = mediaService.getFile(1L, "photo.png");

        assertNotNull(result);
        assertEquals(MediaType.IMAGE_PNG, result.getMediaType());
        assertEquals(mockContent, result.getContent());
    }

    @Test
    void getFile_whenMediaFoundAndNameMatchesCaseInsensitive_thenReturnContent() {
        Media matchingMedia = new Media();
        matchingMedia.setId(2L);
        matchingMedia.setFileName("Photo.PNG");
        matchingMedia.setFilePath("/storage/Photo.PNG");
        matchingMedia.setMediaType("image/png");

        InputStream mockContent = new ByteArrayInputStream(new byte[] {4, 5, 6});

        when(mediaRepository.findById(2L)).thenReturn(Optional.of(matchingMedia));
        when(fileSystemRepository.getFile("/storage/Photo.PNG")).thenReturn(mockContent);

        MediaDto result = mediaService.getFile(2L, "photo.png");

        assertNotNull(result);
        assertEquals(MediaType.IMAGE_PNG, result.getMediaType());
        assertNotNull(result.getContent());
    }

    @Test
    void getMediaByIds_whenEmptyList_thenReturnEmptyList() {
        when(mediaRepository.findAllById(List.of())).thenReturn(List.of());

        List<MediaVm> result = mediaService.getMediaByIds(List.of());

        assertNotNull(result);
        assertThat(result).isEmpty();
    }

    @Test
    void getMediaByIds_whenNoMatchingMedia_thenReturnEmptyList() {
        when(mediaRepository.findAllById(List.of(999L, 1000L))).thenReturn(List.of());

        List<MediaVm> result = mediaService.getMediaByIds(List.of(999L, 1000L));

        assertNotNull(result);
        assertThat(result).isEmpty();
    }

    @Test
    void getMediaByIds_verifyUrlFormat() {
        var media1 = getMedia(1L, "file1.png");
        when(mediaRepository.findAllById(List.of(1L))).thenReturn(List.of(media1));
        when(yasConfig.publicUrl()).thenReturn("https://example.com");

        List<MediaVm> result = mediaService.getMediaByIds(List.of(1L));

        assertFalse(result.isEmpty());
        assertEquals("https://example.com/medias/1/file/file1.png", result.get(0).getUrl());
    }

    @Test
    void getMediaById_verifyAllFieldsAndUrlFormat() {
        NoFileMediaVm noFileMediaVm = new NoFileMediaVm(5L, "My Caption", "photo.jpg", "image/jpeg");
        when(mediaRepository.findByIdWithoutFileInReturn(5L)).thenReturn(noFileMediaVm);
        when(yasConfig.publicUrl()).thenReturn("https://cdn.example.com");

        MediaVm result = mediaService.getMediaById(5L);

        assertNotNull(result);
        assertEquals(5L, result.getId());
        assertEquals("My Caption", result.getCaption());
        assertEquals("photo.jpg", result.getFileName());
        assertEquals("image/jpeg", result.getMediaType());
        assertEquals("https://cdn.example.com/medias/5/file/photo.jpg", result.getUrl());
    }

    private static @NotNull Media getMedia(Long id, String name) {
        var media = new Media();
        media.setId(id);
        media.setFileName(name);
        return media;
    }


}
