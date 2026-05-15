package com.sigae.api.service;

import com.sigae.api.exception.BadRequestException;
import com.sigae.api.exception.ConflictException;
import com.sigae.api.exception.NotFoundException;
import com.sigae.api.model.dto.CreateLoanPayload;
import com.sigae.api.model.dto.LoanActivityResponse;
import com.sigae.api.model.dto.LoanAttachmentFile;
import com.sigae.api.model.dto.LoanDetailResponse;
import com.sigae.api.model.dto.LoanStatusResponse;
import com.sigae.api.model.dto.LoanSummaryResponse;
import com.sigae.api.model.entity.Asset;
import com.sigae.api.model.entity.AssetCondition;
import com.sigae.api.model.entity.AssetTraceability;
import com.sigae.api.model.entity.Loan;
import com.sigae.api.model.entity.LoanAsset;
import com.sigae.api.model.entity.LoanAttachment;
import com.sigae.api.model.entity.LoanAttachmentSource;
import com.sigae.api.model.entity.Location;
import com.sigae.api.model.entity.Teacher;
import com.sigae.api.model.entity.TraceabilityEventType;
import com.sigae.api.model.entity.User;
import com.sigae.api.repository.AssetRepository;
import com.sigae.api.repository.AssetTraceabilityRepository;
import com.sigae.api.repository.LoanAttachmentRepository;
import com.sigae.api.repository.LoanRepository;
import com.sigae.api.repository.LocationRepository;
import com.sigae.api.repository.TeacherRepository;
import com.sigae.api.repository.UserRepository;
import com.sigae.api.security.AuthenticatedUser;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class LoanService {

  private static final Set<AssetCondition> LOANABLE_CONDITIONS = Set.of(AssetCondition.BUENO, AssetCondition.REGULAR);

  private final LoanRepository loanRepository;
  private final LoanAttachmentRepository attachmentRepository;
  private final TeacherRepository teacherRepository;
  private final LocationRepository locationRepository;
  private final AssetRepository assetRepository;
  private final AssetTraceabilityRepository traceabilityRepository;
  private final UserRepository userRepository;

  public LoanService(
      LoanRepository loanRepository,
      LoanAttachmentRepository attachmentRepository,
      TeacherRepository teacherRepository,
      LocationRepository locationRepository,
      AssetRepository assetRepository,
      AssetTraceabilityRepository traceabilityRepository,
      UserRepository userRepository
  ) {
    this.loanRepository = loanRepository;
    this.attachmentRepository = attachmentRepository;
    this.teacherRepository = teacherRepository;
    this.locationRepository = locationRepository;
    this.assetRepository = assetRepository;
    this.traceabilityRepository = traceabilityRepository;
    this.userRepository = userRepository;
  }

  public List<LoanSummaryResponse> findAll(String search, String status) {
    String normalizedSearch = normalizeOptional(search);
    String normalizedStatus = normalizeOptional(status);
    List<Loan> loans = normalizedSearch == null
        ? loanRepository.findAll()
        : loanRepository.search(normalizedSearch);
    return loans.stream()
        .filter(loan -> matchesStatus(loan, normalizedStatus))
        .map(loan -> LoanSummaryResponse.from(loan, statusOf(loan)))
        .toList();
  }

  public LoanDetailResponse getDetail(UUID id) {
    Loan loan = getById(id);
    return LoanDetailResponse.from(loan, statusOf(loan), buildActivities(loan));
  }

  @Transactional
  public LoanDetailResponse create(
      CreateLoanPayload payload,
      MultipartFile signature,
      List<MultipartFile> attachments,
      AuthenticatedUser authenticatedUser
  ) {
    validateCreatePayload(payload);
    User user = findUser(authenticatedUser);
    Teacher teacher = teacherRepository.findById(payload.teacherId())
        .orElseThrow(() -> new NotFoundException("Docente no encontrado."));
    Location destination = locationRepository.findById(payload.destinationLocationId())
        .orElseThrow(() -> new NotFoundException("Ubicación de destino no encontrada."));
    List<Asset> assets = resolveAssets(payload.assetIds());
    validateAssetsCanBeLoaned(assets);

    Loan loan = new Loan(
        buildNextCode(),
        teacher,
        destination,
        payload.loanDate(),
        payload.dueDate(),
        normalizeOptional(payload.notes())
    );
    assets.forEach(asset -> loan.addAsset(new LoanAsset(asset)));
    applySignature(loan, signature);
    applyAttachments(loan, attachments, payload.attachmentSources());

    Loan saved = loanRepository.save(loan);
    assets.forEach(asset -> traceabilityRepository.save(new AssetTraceability(
        asset,
        TraceabilityEventType.LOANED,
        "Activo prestado en el préstamo %s.".formatted(saved.getCode()),
        asset.getLocation().getName(),
        destination.getName(),
        "Docente: %s".formatted(teacher.getFullName()),
        user
    )));

    return getDetail(saved.getId());
  }

  @Transactional
  public LoanDetailResponse returnLoan(UUID id, AuthenticatedUser authenticatedUser) {
    Loan loan = getById(id);
    if (loan.getCompletedAt() != null) {
      throw new ConflictException("El préstamo ya fue devuelto.");
    }

    User user = findUser(authenticatedUser);
    loan.markReturned();
    Loan saved = loanRepository.save(loan);
    saved.getAssets().forEach(loanAsset -> traceabilityRepository.save(new AssetTraceability(
        loanAsset.getAsset(),
        TraceabilityEventType.RETURNED,
        "Activo devuelto del préstamo %s.".formatted(saved.getCode()),
        saved.getDestinationNameSnapshot(),
        loanAsset.getAsset().getLocation().getName(),
        "Devolución registrada.",
        user
    )));

    return getDetail(saved.getId());
  }

  public LoanAttachmentFile getAttachment(UUID loanId, UUID attachmentId) {
    LoanAttachment attachment = attachmentRepository.findByIdAndLoanId(attachmentId, loanId)
        .orElseThrow(() -> new NotFoundException("Adjunto de préstamo no encontrado."));
    return new LoanAttachmentFile(
        attachment.getFileName(),
        MediaType.parseMediaType(attachment.getMimeType()),
        attachment.getContent()
    );
  }

  public Loan getById(UUID id) {
    return loanRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Préstamo no encontrado."));
  }

  public LoanStatusResponse statusOf(Loan loan) {
    if (loan.getCompletedAt() != null) {
      return LoanStatusResponse.DEVUELTO;
    }

    return loan.getDueDate().isBefore(LocalDate.now()) ? LoanStatusResponse.VENCIDO : LoanStatusResponse.ACTIVO;
  }

  public boolean isAssetAvailableForLoan(Asset asset) {
    return LOANABLE_CONDITIONS.contains(asset.getCondition())
        && loanRepository.findActiveLoanByAssetId(asset.getId()).isEmpty();
  }

  public UUID activeLoanIdForAsset(UUID assetId) {
    return loanRepository.findActiveLoanByAssetId(assetId).map(Loan::getId).orElse(null);
  }

  private void validateCreatePayload(CreateLoanPayload payload) {
    if (payload == null) {
      throw new BadRequestException("Datos de préstamo requeridos.");
    }
    if (payload.assetIds() == null || payload.assetIds().isEmpty()) {
      throw new BadRequestException("Debe seleccionar al menos un activo.");
    }
    if (payload.loanDate() == null || payload.dueDate() == null) {
      throw new BadRequestException("Las fechas del préstamo son requeridas.");
    }
    if (payload.dueDate().isBefore(payload.loanDate())) {
      throw new BadRequestException("La fecha de devolución no puede ser anterior a la fecha de inicio.");
    }
  }

  private List<Asset> resolveAssets(List<UUID> assetIds) {
    List<UUID> uniqueIds = new LinkedHashSet<>(assetIds).stream().toList();
    List<Asset> assets = uniqueIds.stream()
        .map(id -> assetRepository.findById(id).orElseThrow(() -> new NotFoundException("Activo no encontrado.")))
        .toList();
    if (assets.size() != assetIds.size()) {
      throw new BadRequestException("La lista de activos contiene duplicados.");
    }
    return assets;
  }

  private void validateAssetsCanBeLoaned(List<Asset> assets) {
    for (Asset asset : assets) {
      if (!LOANABLE_CONDITIONS.contains(asset.getCondition())) {
        throw new ConflictException("El activo %s no está disponible por su estado actual.".formatted(asset.getCode()));
      }
    }

    List<UUID> ids = assets.stream().map(Asset::getId).toList();
    List<Loan> activeLoans = loanRepository.findActiveLoansByAssetIds(ids);
    if (!activeLoans.isEmpty()) {
      throw new ConflictException("Uno o más activos ya pertenecen a un préstamo activo.");
    }
  }

  private void applySignature(Loan loan, MultipartFile signature) {
    if (signature == null || signature.isEmpty()) {
      return;
    }
    String contentType = signature.getContentType() == null ? "image/png" : signature.getContentType();
    if (!contentType.equalsIgnoreCase(MediaType.IMAGE_PNG_VALUE)) {
      throw new BadRequestException("La firma debe enviarse en formato PNG.");
    }
    try {
      loan.setSignature(signature.getBytes(), contentType, normalizeFilename(signature.getOriginalFilename(), "firma-prestamo.png"));
    } catch (IOException exception) {
      throw new BadRequestException("No se pudo procesar la firma digital.");
    }
  }

  private void applyAttachments(Loan loan, List<MultipartFile> attachments, List<String> sources) {
    if (attachments == null || attachments.isEmpty()) {
      return;
    }
    for (int index = 0; index < attachments.size(); index++) {
      MultipartFile file = attachments.get(index);
      if (file == null || file.isEmpty()) {
        continue;
      }
      try {
        loan.addAttachment(new LoanAttachment(
            normalizeFilename(file.getOriginalFilename(), "adjunto-%d".formatted(index + 1)),
            file.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.getContentType(),
            file.getSize(),
            LoanAttachmentSource.fromValue(sources == null || sources.size() <= index ? null : sources.get(index)),
            file.getBytes()
        ));
      } catch (IOException exception) {
        throw new BadRequestException("No se pudo procesar uno de los adjuntos.");
      }
    }
  }

  private List<LoanActivityResponse> buildActivities(Loan loan) {
    LoanActivityResponse created = LoanActivityResponse.of(
        loan.getId(),
        "Préstamo registrado",
        "Se asignaron %d activo%s a %s.".formatted(
            loan.getAssets().size(),
            loan.getAssets().size() == 1 ? "" : "s",
            loan.getTeacherNameSnapshot()
        ),
        "Sistema",
        loan.getCreatedAt()
    );
    if (loan.getCompletedAt() == null) {
      return List.of(created);
    }

    LoanActivityResponse returned = LoanActivityResponse.of(
        loan.getId(),
        "Préstamo devuelto",
        "La devolución del préstamo fue registrada correctamente.",
        "Sistema",
        loan.getCompletedAt()
    );
    return List.of(created, returned).stream()
        .sorted(Comparator.comparing(LoanActivityResponse::timestamp).reversed())
        .toList();
  }

  private boolean matchesStatus(Loan loan, String status) {
    if (status == null || status.equalsIgnoreCase("all")) {
      return true;
    }

    LoanStatusResponse currentStatus = statusOf(loan);
    return switch (status.toLowerCase(Locale.ROOT)) {
      case "active", "activo" -> currentStatus == LoanStatusResponse.ACTIVO;
      case "overdue", "vencido" -> currentStatus == LoanStatusResponse.VENCIDO;
      case "returned", "devuelto" -> currentStatus == LoanStatusResponse.DEVUELTO;
      default -> true;
    };
  }

  private User findUser(AuthenticatedUser authenticatedUser) {
    if (authenticatedUser == null) {
      return null;
    }
    return userRepository.findById(authenticatedUser.userId()).orElse(null);
  }

  private String buildNextCode() {
    return "PRE-%s-%04d".formatted(LocalDate.now().getYear(), loanRepository.count() + 1);
  }

  private String normalizeOptional(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String normalizeFilename(String value, String fallback) {
    String normalized = normalizeOptional(value);
    return normalized == null ? fallback : normalized;
  }
}
