package com.sigae.api.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.sigae.api.exception.BadRequestException;
import com.sigae.api.model.dto.AssetReportRowResponse;
import com.sigae.api.model.dto.LoanReportRowResponse;
import com.sigae.api.model.dto.PhysicalInventoryReportResponse;
import com.sigae.api.model.dto.PhysicalInventoryReportRowResponse;
import com.sigae.api.model.dto.ReportExportFile;
import com.sigae.api.model.dto.ReportExportFormat;
import com.sigae.api.model.entity.Asset;
import com.sigae.api.model.entity.AssetCondition;
import com.sigae.api.model.entity.InstitutionSettings;
import com.sigae.api.model.entity.Loan;
import com.sigae.api.repository.AssetRepository;
import com.sigae.api.repository.CategoryRepository;
import com.sigae.api.repository.LoanRepository;
import com.sigae.api.repository.LocationRepository;
import com.sigae.api.repository.UserRepository;
import com.sigae.api.security.AuthenticatedUser;
import jakarta.persistence.criteria.Predicate;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class ReportService {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final String[] ASSET_HEADERS = {
      "N.°",
      "Código",
      "Categoría",
      "Descripción",
      "Ubicación",
      "Estado",
      "Fecha alta"
  };
  private static final String[] DECOMMISSIONED_ASSET_HEADERS = {
      "N.°",
      "Código",
      "Categoría",
      "Descripción",
      "Ubicación",
      "Estado",
      "Fecha alta",
      "Fecha baja"
  };
  private static final String[] LOAN_HEADERS = {
      "N.°",
      "Código préstamo",
      "Docente",
      "Cant. activos",
      "Fecha préstamo",
      "Fecha límite",
      "Ubicación",
      "Estado"
  };

  private final AssetRepository assetRepository;
  private final LoanRepository loanRepository;
  private final LoanService loanService;
  private final InstitutionSettingsService institutionSettingsService;
  private final UserRepository userRepository;
  private final CategoryRepository categoryRepository;
  private final LocationRepository locationRepository;

  public ReportService(
      AssetRepository assetRepository,
      LoanRepository loanRepository,
      LoanService loanService,
      InstitutionSettingsService institutionSettingsService,
      UserRepository userRepository,
      CategoryRepository categoryRepository,
      LocationRepository locationRepository
  ) {
    this.assetRepository = assetRepository;
    this.loanRepository = loanRepository;
    this.loanService = loanService;
    this.institutionSettingsService = institutionSettingsService;
    this.userRepository = userRepository;
    this.categoryRepository = categoryRepository;
    this.locationRepository = locationRepository;
  }

  public List<AssetReportRowResponse> listAssetRows(
      UUID categoryId,
      UUID locationId,
      LocalDate startDate,
      LocalDate endDate
  ) {
    ensureValidDateRange(startDate, endDate);
    return findAssets(categoryId, locationId, startDate, endDate).stream()
        .map(AssetReportRowResponse::from)
        .toList();
  }

  public ReportExportFile exportAssetRows(
      UUID categoryId,
      UUID locationId,
      LocalDate startDate,
      LocalDate endDate,
      ReportExportFormat format,
      AuthenticatedUser authenticatedUser
  ) {
    return exportAssetRows(categoryId, locationId, startDate, endDate, format, null, authenticatedUser);
  }

  public ReportExportFile exportAssetRows(
      UUID categoryId,
      UUID locationId,
      LocalDate startDate,
      LocalDate endDate,
      ReportExportFormat format,
      MultipartFile signature,
      AuthenticatedUser authenticatedUser
  ) {
    PhysicalInventoryReportResponse report = physicalInventory(
        categoryId,
        locationId,
        startDate,
        endDate,
        authenticatedUser
    );
    PhysicalInventoryExportContext context = new PhysicalInventoryExportContext(
        report,
        institutionSettingsService.getCurrentSettings(),
        readReportSignature(signature)
    );
    byte[] content = switch (format) {
      case PDF -> buildPhysicalInventoryPdf(context);
      case EXCEL -> buildPhysicalInventoryExcel(context);
      case WORD -> buildPhysicalInventoryWord(context);
    };

    return new ReportExportFile(
        "reporte-activos-%s.%s".formatted(LocalDate.now(), format.extension()),
        format.contentType(),
        content
    );
  }

  private ReportSignature readReportSignature(MultipartFile signature) {
    if (signature == null || signature.isEmpty()) {
      return null;
    }
    String contentType = signature.getContentType() == null ? "image/png" : signature.getContentType();
    if (!"image/png".equalsIgnoreCase(contentType)) {
      throw new BadRequestException("La firma debe enviarse en formato PNG.");
    }
    try {
      return new ReportSignature(
          trimTransparentSignaturePadding(signature.getBytes()),
          normalizeText(signature.getOriginalFilename(), "firma-reporte.png")
      );
    } catch (IOException exception) {
      throw new BadRequestException("No se pudo procesar la firma digital del reporte.");
    }
  }

  private byte[] trimTransparentSignaturePadding(byte[] signatureContent) throws IOException {
    BufferedImage source = ImageIO.read(new ByteArrayInputStream(signatureContent));
    if (source == null) {
      throw new IOException("El contenido de la firma no es una imagen PNG válida.");
    }

    int minX = source.getWidth();
    int minY = source.getHeight();
    int maxX = -1;
    int maxY = -1;
    for (int y = 0; y < source.getHeight(); y++) {
      for (int x = 0; x < source.getWidth(); x++) {
        if (((source.getRGB(x, y) >>> 24) & 0xFF) == 0) {
          continue;
        }
        minX = Math.min(minX, x);
        minY = Math.min(minY, y);
        maxX = Math.max(maxX, x);
        maxY = Math.max(maxY, y);
      }
    }
    if (maxX < 0) {
      return signatureContent;
    }

    int padding = 12;
    minX = Math.max(0, minX - padding);
    minY = Math.max(0, minY - padding);
    maxX = Math.min(source.getWidth() - 1, maxX + padding);
    maxY = Math.min(source.getHeight() - 1, maxY + padding);
    BufferedImage cropped = source.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    ImageIO.write(cropped, "png", output);
    return output.toByteArray();
  }

  public PhysicalInventoryReportResponse physicalInventory(
      UUID categoryId,
      UUID locationId,
      LocalDate startDate,
      LocalDate endDate,
      AuthenticatedUser authenticatedUser
  ) {
    ensureValidDateRange(startDate, endDate);
    InstitutionSettings settings = institutionSettingsService.getCurrentSettings();
    String locationName = locationId == null
        ? null
        : locationRepository.findById(locationId).map(location -> location.getName()).orElse(null);
    return new PhysicalInventoryReportResponse(
        buildUgelName(settings.getCity()),
        normalizeText(settings.getSystemName(), "Institución educativa"),
        "INVENTARIO FÍSICO DE BIENES PATRIMONIALES",
        buildLocationSubtitle(locationName),
        resolveGeneratedBy(authenticatedUser).name(),
        LocalDate.now(),
        findAssets(categoryId, locationId, startDate, endDate).stream()
            .map(this::toPhysicalInventoryRow)
            .toList()
    );
  }

  private PhysicalInventoryReportRowResponse toPhysicalInventoryRow(Asset asset) {
    String technicalDescription = normalizeText(asset.getDescription(), "");
    String assetName = normalizeText(asset.getName(), "");
    String assetDescription = technicalDescription.isBlank() || technicalDescription.equalsIgnoreCase(assetName)
        ? assetName
        : assetName + " — " + technicalDescription;
    String brand = asset.getAttributeValues().stream()
        .filter(value -> "marca".equalsIgnoreCase(value.getAttributeDefinition().getName()))
        .map(value -> normalizeText(value.getValue(), ""))
        .filter(value -> !value.isBlank())
        .findFirst()
        .orElse("");
    String observations = joinObservations(conditionSupplement(asset.getCondition()), asset.getNotes());
    return new PhysicalInventoryReportRowResponse(
        asset.getId(),
        asset.getCode(),
        assetDescription,
        asset.getLocation().getName(),
        asset.getCondition() == AssetCondition.BUENO,
        asset.getCondition() == AssetCondition.REGULAR,
        asset.getCondition() == AssetCondition.MALO,
        brand,
        observations,
        1
    );
  }

  private String buildUgelName(String city) {
    String normalizedCity = normalizeText(city, "");
    return normalizedCity.isBlank()
        ? null
        : "UNIDAD DE GESTIÓN EDUCATIVA LOCAL DE " + normalizedCity.toUpperCase(Locale.ROOT);
  }

  private String buildLocationSubtitle(String locationName) {
    String normalizedLocation = normalizeText(locationName, "");
    if (normalizedLocation.isBlank()) {
      return null;
    }
    return "AIP".equalsIgnoreCase(normalizedLocation)
        ? "AULA DE INNOVACIÓN PEDAGÓGICA"
        : normalizedLocation.toUpperCase(Locale.ROOT);
  }

  private String conditionSupplement(AssetCondition condition) {
    return switch (condition) {
      case MANTENIMIENTO -> "Estado del sistema: Mantenimiento";
      case DADO_DE_BAJA -> "Estado del sistema: Dado de baja";
      default -> "";
    };
  }

  private String joinObservations(String prefix, String observations) {
    String normalizedPrefix = normalizeText(prefix, "");
    String normalizedObservations = normalizeText(observations, "");
    if (normalizedPrefix.isBlank()) {
      return normalizedObservations;
    }
    return normalizedObservations.isBlank() ? normalizedPrefix : normalizedPrefix + " | " + normalizedObservations;
  }

  public List<LoanReportRowResponse> listLoanRows(
      String search,
      UUID locationId,
      LocalDate startDate,
      LocalDate endDate
  ) {
    ensureValidDateRange(startDate, endDate);
    String normalizedSearch = search == null || search.isBlank() ? null : search.trim().toLowerCase(Locale.ROOT);
    return loanRepository.findAll().stream()
        .filter(loan -> locationId == null || loan.getDestinationLocation().getId().equals(locationId))
        .filter(loan -> matchesDateRange(loan.getLoanDate(), startDate, endDate))
        .filter(loan -> matchesLoanSearch(loan, normalizedSearch))
        .map(loan -> LoanReportRowResponse.from(loan, loanService.statusOf(loan)))
        .toList();
  }

  public ReportExportFile exportLoanRows(
      String search,
      UUID locationId,
      LocalDate startDate,
      LocalDate endDate,
      ReportExportFormat format,
      AuthenticatedUser authenticatedUser
  ) {
    List<LoanReportRowResponse> rows = listLoanRows(search, locationId, startDate, endDate);
    ReportDocumentContext context = buildLoanContext(search, locationId, startDate, endDate, authenticatedUser);
    byte[] content = switch (format) {
      case PDF -> buildLoansPdf(rows, context);
      case EXCEL -> buildLoansExcel(rows, context);
      case WORD -> buildLoansWord(rows, context);
    };

    return new ReportExportFile(
        "reporte-prestamos-%s.%s".formatted(LocalDate.now(), format.extension()),
        format.contentType(),
        content
    );
  }

  private List<Asset> findAssets(
      UUID categoryId,
      UUID locationId,
      LocalDate startDate,
      LocalDate endDate
  ) {
    return assetRepository.findAll(
        assetReportSpecification(categoryId, locationId, startDate, endDate),
        Sort.by(Sort.Direction.ASC, "code")
    );
  }

  private Specification<Asset> assetReportSpecification(
      UUID categoryId,
      UUID locationId,
      LocalDate startDate,
      LocalDate endDate
  ) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      if (categoryId != null) {
        predicates.add(criteriaBuilder.equal(root.get("assetType").get("category").get("id"), categoryId));
      }

      if (locationId != null) {
        predicates.add(criteriaBuilder.equal(root.get("location").get("id"), locationId));
      }

      if (startDate != null) {
        predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("acquisitionDate"), startDate));
      }

      if (endDate != null) {
        predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("acquisitionDate"), endDate));
      }

      return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    };
  }

  private ReportDocumentContext buildAssetContext(
      UUID categoryId,
      UUID locationId,
      LocalDate startDate,
      LocalDate endDate,
      AuthenticatedUser authenticatedUser
  ) {
    InstitutionSettings settings = institutionSettingsService.getCurrentSettings();
    ReportGeneratedBy generatedBy = resolveGeneratedBy(authenticatedUser);
    List<ReportFilterLine> filters = List.of(
        new ReportFilterLine("Categoría", categoryId == null ? "Todas" : categoryRepository.findById(categoryId).map(category -> category.getName()).orElse("No disponible")),
        new ReportFilterLine("Ubicación", locationId == null ? "Todas" : locationRepository.findById(locationId).map(location -> location.getName()).orElse("No disponible")),
        new ReportFilterLine("Rango de fechas", formatDateRange(startDate, endDate))
    );

    return new ReportDocumentContext(
        "INFORME DE INVENTARIO DE ACTIVOS",
        "El presente informe detalla los bienes registrados en el inventario institucional, organizados por categoría, ubicación y estado. Este documento permite sustentar la disponibilidad de recursos, facilitar el control patrimonial y apoyar la toma de decisiones administrativas.",
        "Los bienes descritos en el presente reporte quedan registrados para fines de control, seguimiento y conservación institucional.",
        settings,
        generatedBy,
        filters
    );
  }

  private ReportDocumentContext buildLoanContext(
      String search,
      UUID locationId,
      LocalDate startDate,
      LocalDate endDate,
      AuthenticatedUser authenticatedUser
  ) {
    InstitutionSettings settings = institutionSettingsService.getCurrentSettings();
    ReportGeneratedBy generatedBy = resolveGeneratedBy(authenticatedUser);
    List<ReportFilterLine> filters = List.of(
        new ReportFilterLine("Búsqueda", search == null || search.isBlank() ? "Todas" : search.trim()),
        new ReportFilterLine("Ubicación", locationId == null ? "Todas" : locationRepository.findById(locationId).map(location -> location.getName()).orElse("No disponible")),
        new ReportFilterLine("Rango de fechas", formatDateRange(startDate, endDate))
    );

    return new ReportDocumentContext(
        "INFORME DE PRÉSTAMOS DE ACTIVOS",
        "El presente informe detalla los préstamos de activos registrados en el sistema, incluyendo docente responsable, cantidad de bienes, fechas de control, ubicación de destino y estado actual. Este documento permite realizar seguimiento operativo y administrativo de los bienes prestados.",
        "Los préstamos descritos en el presente reporte quedan registrados para fines de control, seguimiento y verificación administrativa.",
        settings,
        generatedBy,
        filters
    );
  }

  private ReportGeneratedBy resolveGeneratedBy(AuthenticatedUser authenticatedUser) {
    if (authenticatedUser == null) {
      return new ReportGeneratedBy("Usuario del sistema", "", "");
    }

    if (authenticatedUser.userId() == null) {
      return new ReportGeneratedBy(
          normalizeText(authenticatedUser.email(), "Usuario del sistema"),
          normalizeText(authenticatedUser.email(), ""),
          authenticatedUser.role() == null ? "" : authenticatedUser.role().getLabel()
      );
    }

    return userRepository.findById(authenticatedUser.userId())
        .map(user -> new ReportGeneratedBy(
            normalizeText(user.getFullName(), "Usuario del sistema"),
            normalizeText(user.getEmail(), ""),
            user.getRole() == null ? "" : user.getRole().getLabel()
        ))
        .orElseGet(() -> new ReportGeneratedBy(
            normalizeText(authenticatedUser.email(), "Usuario del sistema"),
            normalizeText(authenticatedUser.email(), ""),
            authenticatedUser.role() == null ? "" : authenticatedUser.role().getLabel()
        ));
  }

  private byte[] buildAssetsPdf(List<AssetReportRowResponse> rows, ReportDocumentContext context) {
    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      AssetReportSections sections = splitAssetRows(rows);
      Document document = new Document(PageSize.A4.rotate(), 36, 36, 32, 36);
      PdfWriter.getInstance(document, output);
      document.open();
      addPdfReportHeader(document, context);
      addPdfInformativeData(document, context);
      addPdfSection(document, "II. Introducción", context.introduction());
      addPdfTitle(document, "III. Detalle del reporte", 12, Element.ALIGN_LEFT, 8);
      document.add(buildAssetPdfTable(sections.activeRows(), ASSET_HEADERS, new float[] { 0.6f, 1.35f, 1.4f, 2.4f, 1.45f, 1.1f, 1.0f }));
      if (!sections.decommissionedRows().isEmpty()) {
        addPdfTitle(document, "Activos dados de baja", 11, Element.ALIGN_LEFT, 8);
        document.add(buildDecommissionedAssetPdfTable(sections.decommissionedRows()));
      }
      addPdfClosingAndSignature(document, context);
      document.close();
      return output.toByteArray();
    } catch (Exception exception) {
      throw new IllegalStateException("No se pudo generar el reporte PDF.", exception);
    }
  }

  private byte[] buildPhysicalInventoryPdf(PhysicalInventoryExportContext context) {
    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      Document document = new Document(PageSize.A4.rotate(), 22, 22, 24, 24);
      PdfWriter.getInstance(document, output);
      document.open();
      addPhysicalPdfHeading(document, context.report(), context.settings());
      PdfPTable table = new PdfPTable(new float[] {0.45f, 3.3f, 1.0f, 0.35f, 0.35f, 0.35f, 0.85f, 1.45f, 0.5f});
      table.setWidthPercentage(100);
      addPhysicalPdfHeader(table);
      for (int index = 0; index < context.report().rows().size(); index++) {
        addPhysicalPdfRow(table, index + 1, context.report().rows().get(index));
      }
      addEmptyPdfRowIfNeeded(table, 9, context.report().rows().isEmpty());
      document.add(table);
      addPhysicalPdfSignature(document, context.report().generatedBy(), context.signature());
      document.close();
      return output.toByteArray();
    } catch (Exception exception) {
      throw new IllegalStateException("No se pudo generar el inventario físico en PDF.", exception);
    }
  }

  private void addPhysicalPdfHeading(
      Document document,
      PhysicalInventoryReportResponse report,
      InstitutionSettings settings
  ) throws DocumentException, IOException {
    Font institutionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    Font metadataFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
    Image logo = buildPdfLogo(settings);
    if (logo != null) {
      logo.scaleToFit(48, 48);
      logo.setAlignment(Image.ALIGN_CENTER);
      document.add(logo);
    }
    addCenteredPdfLine(document, report.ugelName(), institutionFont);
    addCenteredPdfLine(document, report.institutionName().toUpperCase(Locale.ROOT), institutionFont);
    addCenteredPdfLine(document, report.title(), titleFont);
    addCenteredPdfLine(document, report.locationSubtitle(), institutionFont);
    addCenteredPdfLine(document, "Generado por: " + report.generatedBy() + "  |  Fecha: " + formatDate(report.generatedAt()), metadataFont);
  }

  private void addPhysicalPdfSignature(Document document, String generatedBy, ReportSignature signature) throws DocumentException {
    PdfPTable signatureTable = new PdfPTable(1);
    signatureTable.setWidthPercentage(38);
    signatureTable.setHorizontalAlignment(Element.ALIGN_CENTER);
    signatureTable.setSpacingBefore(28);
    PdfPCell signatureArea = new PdfPCell();
    signatureArea.setBorder(PdfPCell.NO_BORDER);
    signatureArea.setFixedHeight(54);
    signatureArea.setHorizontalAlignment(Element.ALIGN_CENTER);
    signatureArea.setVerticalAlignment(Element.ALIGN_BOTTOM);
    if (signature != null) {
      try {
        Image signatureImage = Image.getInstance(signature.content());
        signatureImage.scaleToFit(160, 44);
        signatureImage.setAlignment(Image.ALIGN_CENTER);
        signatureArea.addElement(signatureImage);
      } catch (Exception exception) {
        throw new IllegalStateException("No se pudo insertar la firma digital en el PDF.", exception);
      }
    }
    signatureTable.addCell(signatureArea);
    PdfPCell line = new PdfPCell(new Phrase(" "));
    line.setBorder(PdfPCell.TOP);
    line.setFixedHeight(8);
    signatureTable.addCell(line);
    PdfPCell name = pdfCell(signatureLabel(generatedBy), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9), Element.ALIGN_CENTER);
    name.setBorder(PdfPCell.NO_BORDER);
    signatureTable.addCell(name);
    document.add(signatureTable);
  }

  private void addCenteredPdfLine(Document document, String value, Font font) throws DocumentException {
    if (value == null || value.isBlank()) {
      return;
    }
    Paragraph line = new Paragraph(value, font);
    line.setAlignment(Element.ALIGN_CENTER);
    line.setSpacingAfter(3);
    document.add(line);
  }

  private void addPhysicalPdfHeader(PdfPTable table) {
    Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7);
    addPhysicalPdfHeaderCell(table, "ITEM", font, 1, 2);
    addPhysicalPdfHeaderCell(table, "DESCRIPCIÓN DEL BIEN", font, 1, 2);
    addPhysicalPdfHeaderCell(table, "UBICACIÓN", font, 1, 2);
    addPhysicalPdfHeaderCell(table, "ESTADO", font, 3, 1);
    addPhysicalPdfHeaderCell(table, "MARCA", font, 1, 2);
    addPhysicalPdfHeaderCell(table, "OBSERVACIONES", font, 1, 2);
    addPhysicalPdfHeaderCell(table, "CANTIDAD", font, 1, 2);
    addPhysicalPdfHeaderCell(table, "B", font, 1, 1);
    addPhysicalPdfHeaderCell(table, "R", font, 1, 1);
    addPhysicalPdfHeaderCell(table, "M", font, 1, 1);
  }

  private void addPhysicalPdfHeaderCell(PdfPTable table, String value, Font font, int colspan, int rowspan) {
    PdfPCell cell = pdfCell(value, font, Element.ALIGN_CENTER);
    cell.setColspan(colspan);
    cell.setRowspan(rowspan);
    cell.setPadding(4);
    table.addCell(cell);
  }

  private void addPhysicalPdfRow(PdfPTable table, int index, PhysicalInventoryReportRowResponse row) {
    Font font = FontFactory.getFont(FontFactory.HELVETICA, 7);
    table.addCell(pdfCell(String.valueOf(index), font, Element.ALIGN_CENTER));
    table.addCell(pdfCell(row.assetDescription(), font, Element.ALIGN_LEFT));
    table.addCell(pdfCell(row.location(), font, Element.ALIGN_CENTER));
    table.addCell(pdfCell(row.good() ? "X" : "", font, Element.ALIGN_CENTER));
    table.addCell(pdfCell(row.regular() ? "X" : "", font, Element.ALIGN_CENTER));
    table.addCell(pdfCell(row.bad() ? "X" : "", font, Element.ALIGN_CENTER));
    table.addCell(pdfCell(row.brand(), font, Element.ALIGN_CENTER));
    table.addCell(pdfCell(row.observations(), font, Element.ALIGN_LEFT));
    table.addCell(pdfCell(String.valueOf(row.quantity()), font, Element.ALIGN_CENTER));
  }

  private byte[] buildLoansPdf(List<LoanReportRowResponse> rows, ReportDocumentContext context) {
    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      Document document = new Document(PageSize.A4.rotate(), 36, 36, 32, 36);
      PdfWriter.getInstance(document, output);
      document.open();
      addPdfReportHeader(document, context);
      addPdfInformativeData(document, context);
      addPdfSection(document, "II. Introducción", context.introduction());
      addPdfTitle(document, "III. Detalle del reporte", 12, Element.ALIGN_LEFT, 8);
      PdfPTable table = new PdfPTable(LOAN_HEADERS.length);
      table.setWidthPercentage(100);
      table.setWidths(new float[] { 0.55f, 1.35f, 2.0f, 0.9f, 1.05f, 1.05f, 1.6f, 1.0f });
      addPdfHeader(table, LOAN_HEADERS);
      for (int index = 0; index < rows.size(); index++) {
        addLoanPdfRow(table, index + 1, rows.get(index));
      }
      addEmptyPdfRowIfNeeded(table, LOAN_HEADERS.length, rows.isEmpty());
      document.add(table);
      addPdfClosingAndSignature(document, context);
      document.close();
      return output.toByteArray();
    } catch (DocumentException | IOException exception) {
      throw new IllegalStateException("No se pudo generar el reporte PDF.", exception);
    }
  }

  private void addPdfReportHeader(Document document, ReportDocumentContext context) throws DocumentException, IOException {
    PdfPTable header = new PdfPTable(new float[] { 1.0f, 5.0f });
    header.setWidthPercentage(100);

    PdfPCell logoCell = new PdfPCell();
    logoCell.setBorder(PdfPCell.NO_BORDER);
    logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
    logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
    Image logo = buildPdfLogo(context.settings());
    if (logo != null) {
      logo.scaleToFit(58, 58);
      logoCell.addElement(logo);
    } else {
      logoCell.addElement(new Paragraph(" "));
    }
    header.addCell(logoCell);

    Font institutionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
    Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
    PdfPCell textCell = new PdfPCell();
    textCell.setBorder(PdfPCell.NO_BORDER);
    textCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
    Paragraph institution = new Paragraph(normalizeText(context.settings().getSystemName(), "Sistema de gestión de activos").toUpperCase(Locale.ROOT), institutionFont);
    institution.setAlignment(Element.ALIGN_CENTER);
    textCell.addElement(institution);
    String locationLine = buildInstitutionLocationLine(context.settings());
    if (!locationLine.isBlank()) {
      Paragraph location = new Paragraph(locationLine, metaFont);
      location.setAlignment(Element.ALIGN_CENTER);
      textCell.addElement(location);
    }
    Paragraph date = new Paragraph("Fecha de emisión: " + formatDate(LocalDate.now()), metaFont);
    date.setAlignment(Element.ALIGN_CENTER);
    textCell.addElement(date);
    header.addCell(textCell);

    document.add(header);
    addPdfTitle(document, context.title(), 14, Element.ALIGN_CENTER, 16);
  }

  private Image buildPdfLogo(InstitutionSettings settings) throws IOException {
    if (!settings.hasLogo()) {
      return null;
    }

    try {
      return Image.getInstance(settings.getLogoContent());
    } catch (Exception ignored) {
      return null;
    }
  }

  private void addPdfInformativeData(Document document, ReportDocumentContext context) throws DocumentException {
    addPdfTitle(document, "I. Datos informativos", 12, Element.ALIGN_LEFT, 6);
    PdfPTable table = new PdfPTable(new float[] { 1.4f, 4.6f });
    table.setWidthPercentage(100);
    addPdfInfoRow(table, "Institución educativa", normalizeText(context.settings().getSystemName(), "No registrado"));
    addPdfInfoRow(table, "Ciudad", normalizeText(context.settings().getCity(), "No registrado"));
    addPdfInfoRow(table, "Dirección", normalizeText(context.settings().getAddress(), "No registrado"));
    addPdfInfoRow(table, "Emitido por", context.generatedBy().name());
    for (ReportFilterLine filter : context.filters()) {
      addPdfInfoRow(table, filter.label(), filter.value());
    }
    document.add(table);
  }

  private void addPdfSection(Document document, String title, String body) throws DocumentException {
    addPdfTitle(document, title, 12, Element.ALIGN_LEFT, 6);
    Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
    Paragraph paragraph = new Paragraph(body, bodyFont);
    paragraph.setAlignment(Element.ALIGN_JUSTIFIED);
    paragraph.setLeading(14);
    paragraph.setSpacingAfter(14);
    document.add(paragraph);
  }

  private void addPdfTitle(Document document, String value, int size, int alignment, int spacingAfter) throws DocumentException {
    Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, size);
    Paragraph title = new Paragraph(value, titleFont);
    title.setAlignment(alignment);
    title.setSpacingAfter(spacingAfter);
    document.add(title);
  }

  private void addPdfInfoRow(PdfPTable table, String label, String value) {
    Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
    table.addCell(pdfCell(label + ":", labelFont, Element.ALIGN_LEFT));
    table.addCell(pdfCell(value, valueFont, Element.ALIGN_LEFT));
  }

  private void addPdfHeader(PdfPTable table, String[] headers) {
    Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
    for (String header : headers) {
      PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
      cell.setPadding(6);
      cell.setHorizontalAlignment(Element.ALIGN_CENTER);
      cell.setBackgroundColor(new Color(219, 234, 254));
      table.addCell(cell);
    }
  }

  private void addAssetPdfRow(PdfPTable table, int index, AssetReportRowResponse row) {
    Font font = FontFactory.getFont(FontFactory.HELVETICA, 8);
    table.addCell(pdfCell(String.valueOf(index), font, Element.ALIGN_CENTER));
    table.addCell(pdfCell(row.code(), font, Element.ALIGN_LEFT));
    table.addCell(pdfCell(row.category(), font, Element.ALIGN_LEFT));
    table.addCell(pdfCell(row.description(), font, Element.ALIGN_LEFT));
    table.addCell(pdfCell(row.location(), font, Element.ALIGN_LEFT));
    table.addCell(pdfCell(row.condition().getLabel(), font, Element.ALIGN_CENTER));
    table.addCell(pdfCell(formatDate(row.acquisitionDate()), font, Element.ALIGN_CENTER));
  }

  private void addDecommissionedAssetPdfRow(PdfPTable table, int index, AssetReportRowResponse row) {
    Font font = FontFactory.getFont(FontFactory.HELVETICA, 8);
    table.addCell(pdfCell(String.valueOf(index), font, Element.ALIGN_CENTER));
    table.addCell(pdfCell(row.code(), font, Element.ALIGN_LEFT));
    table.addCell(pdfCell(row.category(), font, Element.ALIGN_LEFT));
    table.addCell(pdfCell(row.description(), font, Element.ALIGN_LEFT));
    table.addCell(pdfCell(row.location(), font, Element.ALIGN_LEFT));
    table.addCell(pdfCell(row.condition().getLabel(), font, Element.ALIGN_CENTER));
    table.addCell(pdfCell(formatDate(row.acquisitionDate()), font, Element.ALIGN_CENTER));
    table.addCell(pdfCell(formatInstantDate(row.decommissionedAt(), "No registrada"), font, Element.ALIGN_CENTER));
  }

  private void addLoanPdfRow(PdfPTable table, int index, LoanReportRowResponse row) {
    Font font = FontFactory.getFont(FontFactory.HELVETICA, 8);
    table.addCell(pdfCell(String.valueOf(index), font, Element.ALIGN_CENTER));
    table.addCell(pdfCell(row.code(), font, Element.ALIGN_LEFT));
    table.addCell(pdfCell(row.teacherName(), font, Element.ALIGN_LEFT));
    table.addCell(pdfCell(String.valueOf(row.assetsCount()), font, Element.ALIGN_CENTER));
    table.addCell(pdfCell(formatDate(row.loanDate()), font, Element.ALIGN_CENTER));
    table.addCell(pdfCell(formatDate(row.dueDate()), font, Element.ALIGN_CENTER));
    table.addCell(pdfCell(row.location(), font, Element.ALIGN_LEFT));
    table.addCell(pdfCell(row.status(), font, Element.ALIGN_CENTER));
  }

  private PdfPCell pdfCell(String value, Font font, int alignment) {
    PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value, font));
    cell.setPadding(5);
    cell.setHorizontalAlignment(alignment);
    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
    return cell;
  }

  private void addEmptyPdfRowIfNeeded(PdfPTable table, int colspan, boolean empty) {
    if (!empty) {
      return;
    }
    Font font = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9);
    PdfPCell cell = pdfCell("No hay registros para los filtros aplicados.", font, Element.ALIGN_CENTER);
    cell.setColspan(colspan);
    table.addCell(cell);
  }

  private void addPdfClosingAndSignature(Document document, ReportDocumentContext context) throws DocumentException {
    Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
    Paragraph closing = new Paragraph(context.closingText() + "\n\nAtentamente,", bodyFont);
    closing.setSpacingBefore(18);
    closing.setAlignment(Element.ALIGN_LEFT);
    document.add(closing);

    PdfPTable signature = new PdfPTable(1);
    signature.setWidthPercentage(38);
    signature.setHorizontalAlignment(Element.ALIGN_CENTER);
    signature.setSpacingBefore(34);
    PdfPCell line = new PdfPCell(new Phrase(" "));
    line.setBorder(PdfPCell.TOP);
    line.setFixedHeight(8);
    signature.addCell(line);
    PdfPCell name = pdfCell(context.generatedBy().name(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9), Element.ALIGN_CENTER);
    name.setBorder(PdfPCell.NO_BORDER);
    signature.addCell(name);
    document.add(signature);
  }

  private byte[] buildAssetsExcel(List<AssetReportRowResponse> rows, ReportDocumentContext context) {
    try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      AssetReportSections sections = splitAssetRows(rows);
      var sheet = workbook.createSheet("Activos");
      int rowIndex = writeExcelMetadata(sheet, workbook, context);
      writeExcelHeader(sheet.createRow(rowIndex++), workbook, ASSET_HEADERS);
      for (int index = 0; index < sections.activeRows().size(); index++) {
        Row row = sheet.createRow(rowIndex++);
        writeAssetExcelRow(row, index + 1, sections.activeRows().get(index));
      }
      if (!sections.decommissionedRows().isEmpty()) {
        rowIndex++;
        Row sectionTitleRow = sheet.createRow(rowIndex++);
        sectionTitleRow.createCell(0).setCellValue("Activos dados de baja");
        writeExcelHeader(sheet.createRow(rowIndex++), workbook, DECOMMISSIONED_ASSET_HEADERS);
        for (int index = 0; index < sections.decommissionedRows().size(); index++) {
          Row row = sheet.createRow(rowIndex++);
          writeDecommissionedAssetExcelRow(row, index + 1, sections.decommissionedRows().get(index));
        }
      }
      autosize(sheet, DECOMMISSIONED_ASSET_HEADERS.length);
      workbook.write(output);
      return output.toByteArray();
    } catch (IOException exception) {
      throw new IllegalStateException("No se pudo generar el reporte Excel.", exception);
    }
  }

  private byte[] buildPhysicalInventoryExcel(PhysicalInventoryExportContext context) {
    try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      var sheet = workbook.createSheet("Inventario físico");
      CellStyle centered = physicalExcelStyle(workbook, true, true);
      CellStyle left = physicalExcelStyle(workbook, false, true);
      int rowIndex = 0;
      rowIndex = writePhysicalExcelTitle(sheet, rowIndex, context.report(), context.settings(), workbook);
      Row header = sheet.createRow(rowIndex);
      Row subheader = sheet.createRow(rowIndex + 1);
      writePhysicalExcelHeader(sheet, header, subheader, centered);
      rowIndex += 2;
      for (int index = 0; index < context.report().rows().size(); index++) {
        PhysicalInventoryReportRowResponse reportRow = context.report().rows().get(index);
        Row row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue(index + 1);
        row.createCell(1).setCellValue(reportRow.assetDescription());
        row.createCell(2).setCellValue(reportRow.location());
        row.createCell(3).setCellValue(reportRow.good() ? "X" : "");
        row.createCell(4).setCellValue(reportRow.regular() ? "X" : "");
        row.createCell(5).setCellValue(reportRow.bad() ? "X" : "");
        row.createCell(6).setCellValue(reportRow.brand());
        row.createCell(7).setCellValue(reportRow.observations());
        row.createCell(8).setCellValue(reportRow.quantity());
        for (int column = 0; column < 9; column++) {
          row.getCell(column).setCellStyle(column == 1 || column == 7 ? left : centered);
        }
      }
      sheet.setColumnWidth(0, 1500);
      sheet.setColumnWidth(1, 26000);
      sheet.setColumnWidth(2, 4200);
      sheet.setColumnWidth(3, 1200);
      sheet.setColumnWidth(4, 1200);
      sheet.setColumnWidth(5, 1200);
      sheet.setColumnWidth(6, 4600);
      sheet.setColumnWidth(7, 9000);
      sheet.setColumnWidth(8, 1700);
      addPhysicalExcelSignature(sheet, rowIndex, context.report().generatedBy(), context.signature(), workbook);
      workbook.write(output);
      return output.toByteArray();
    } catch (IOException exception) {
      throw new IllegalStateException("No se pudo generar el inventario físico en Excel.", exception);
    }
  }

  private int writePhysicalExcelTitle(
      org.apache.poi.ss.usermodel.Sheet sheet,
      int rowIndex,
      PhysicalInventoryReportResponse report,
      InstitutionSettings settings,
      XSSFWorkbook workbook
  ) {
    CellStyle titleStyle = workbook.createCellStyle();
    org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
    titleFont.setBold(true);
    titleFont.setFontHeightInPoints((short) 11);
    titleStyle.setFont(titleFont);
    titleStyle.setAlignment(HorizontalAlignment.CENTER);
    boolean hasLogo = settings.hasLogo();
    if (hasLogo) {
      addPhysicalExcelLogo(sheet, rowIndex, settings, workbook);
    }
    String[] lines = {report.ugelName(), report.institutionName(), report.title(), report.locationSubtitle()};
    for (String line : lines) {
      if (line == null || line.isBlank()) {
        continue;
      }
      int titleColumn = hasLogo ? 1 : 0;
      Row row = sheet.createRow(rowIndex);
      row.createCell(titleColumn).setCellValue(line);
      row.getCell(titleColumn).setCellStyle(titleStyle);
      sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, hasLogo ? 1 : 0, 8));
      rowIndex++;
    }
    Row metadata = sheet.createRow(rowIndex++);
    metadata.createCell(0).setCellValue("Generado por: " + report.generatedBy());
    metadata.createCell(5).setCellValue("Fecha: " + formatDate(report.generatedAt()));
    return rowIndex + 1;
  }

  private void addPhysicalExcelLogo(
      org.apache.poi.ss.usermodel.Sheet sheet,
      int rowIndex,
      InstitutionSettings settings,
      XSSFWorkbook workbook
  ) {
    CreationHelper helper = workbook.getCreationHelper();
    Drawing<?> drawing = sheet.createDrawingPatriarch();
    ClientAnchor anchor = helper.createClientAnchor();
    anchor.setCol1(0);
    anchor.setCol2(1);
    anchor.setRow1(rowIndex);
    anchor.setRow2(rowIndex + 4);
    int pictureIndex = workbook.addPicture(settings.getLogoContent(), excelPictureType(settings.getLogoMimeType()));
    drawing.createPicture(anchor, pictureIndex);
  }

  private void addPhysicalExcelSignature(
      org.apache.poi.ss.usermodel.Sheet sheet,
      int rowIndex,
      String generatedBy,
      ReportSignature signature,
      XSSFWorkbook workbook
  ) {
    int signatureRow = rowIndex + 2;
    if (signature != null) {
      CreationHelper helper = workbook.getCreationHelper();
      Drawing<?> drawing = sheet.createDrawingPatriarch();
      ClientAnchor anchor = helper.createClientAnchor();
      anchor.setCol1(3);
      anchor.setCol2(6);
      anchor.setRow1(signatureRow);
      anchor.setRow2(signatureRow + 3);
      int pictureIndex = workbook.addPicture(signature.content(), XSSFWorkbook.PICTURE_TYPE_PNG);
      drawing.createPicture(anchor, pictureIndex);
    }

    Row lineRow = sheet.createRow(signatureRow + 3);
    CellStyle lineStyle = workbook.createCellStyle();
    lineStyle.setBorderTop(BorderStyle.THIN);
    lineStyle.setAlignment(HorizontalAlignment.CENTER);
    for (int column = 3; column <= 5; column++) {
      lineRow.createCell(column).setCellStyle(lineStyle);
    }
    sheet.addMergedRegion(new CellRangeAddress(signatureRow + 3, signatureRow + 3, 3, 5));

    Row nameRow = sheet.createRow(signatureRow + 4);
    CellStyle nameStyle = workbook.createCellStyle();
    org.apache.poi.ss.usermodel.Font nameFont = workbook.createFont();
    nameFont.setBold(true);
    nameStyle.setFont(nameFont);
    nameStyle.setAlignment(HorizontalAlignment.CENTER);
    nameRow.createCell(3).setCellValue(signatureLabel(generatedBy));
    nameRow.getCell(3).setCellStyle(nameStyle);
    sheet.addMergedRegion(new CellRangeAddress(signatureRow + 4, signatureRow + 4, 3, 5));
  }

  private int excelPictureType(String mimeType) {
    return "image/png".equalsIgnoreCase(mimeType)
        ? XSSFWorkbook.PICTURE_TYPE_PNG
        : XSSFWorkbook.PICTURE_TYPE_JPEG;
  }

  private CellStyle physicalExcelStyle(XSSFWorkbook workbook, boolean centered, boolean bordered) {
    CellStyle style = workbook.createCellStyle();
    style.setAlignment(centered ? HorizontalAlignment.CENTER : HorizontalAlignment.LEFT);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    style.setWrapText(true);
    if (bordered) {
      style.setBorderTop(BorderStyle.THIN);
      style.setBorderBottom(BorderStyle.THIN);
      style.setBorderLeft(BorderStyle.THIN);
      style.setBorderRight(BorderStyle.THIN);
    }
    return style;
  }

  private void writePhysicalExcelHeader(
      org.apache.poi.ss.usermodel.Sheet sheet,
      Row header,
      Row subheader,
      CellStyle style
  ) {
    String[] labels = {"ITEM", "DESCRIPCIÓN DEL BIEN", "UBICACIÓN", "ESTADO", "", "", "MARCA", "OBSERVACIONES", "CANTIDAD"};
    for (int column = 0; column < labels.length; column++) {
      header.createCell(column).setCellValue(labels[column]);
      header.getCell(column).setCellStyle(style);
      subheader.createCell(column).setCellStyle(style);
    }
    subheader.getCell(3).setCellValue("B");
    subheader.getCell(4).setCellValue("R");
    subheader.getCell(5).setCellValue("M");
    for (int column : new int[] {0, 1, 2, 6, 7, 8}) {
      sheet.addMergedRegion(new CellRangeAddress(header.getRowNum(), subheader.getRowNum(), column, column));
    }
    sheet.addMergedRegion(new CellRangeAddress(header.getRowNum(), header.getRowNum(), 3, 5));
  }

  private byte[] buildLoansExcel(List<LoanReportRowResponse> rows, ReportDocumentContext context) {
    try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      var sheet = workbook.createSheet("Préstamos");
      int rowIndex = writeExcelMetadata(sheet, workbook, context);
      writeExcelHeader(sheet.createRow(rowIndex++), workbook, LOAN_HEADERS);
      for (int index = 0; index < rows.size(); index++) {
        Row row = sheet.createRow(rowIndex++);
        writeLoanExcelRow(row, index + 1, rows.get(index));
      }
      autosize(sheet, LOAN_HEADERS.length);
      workbook.write(output);
      return output.toByteArray();
    } catch (IOException exception) {
      throw new IllegalStateException("No se pudo generar el reporte Excel.", exception);
    }
  }

  private int writeExcelMetadata(org.apache.poi.ss.usermodel.Sheet sheet, XSSFWorkbook workbook, ReportDocumentContext context) {
    CellStyle titleStyle = workbook.createCellStyle();
    org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
    titleFont.setBold(true);
    titleFont.setFontHeightInPoints((short) 13);
    titleStyle.setFont(titleFont);

    int rowIndex = 0;
    Row titleRow = sheet.createRow(rowIndex++);
    var titleCell = titleRow.createCell(0);
    titleCell.setCellValue(context.title());
    titleCell.setCellStyle(titleStyle);

    rowIndex = writeExcelInfoRow(sheet, rowIndex, "Institución", normalizeText(context.settings().getSystemName(), "No registrado"));
    rowIndex = writeExcelInfoRow(sheet, rowIndex, "Ciudad", normalizeText(context.settings().getCity(), "No registrado"));
    rowIndex = writeExcelInfoRow(sheet, rowIndex, "Dirección", normalizeText(context.settings().getAddress(), "No registrado"));
    rowIndex = writeExcelInfoRow(sheet, rowIndex, "Emitido por", context.generatedBy().name());
    rowIndex = writeExcelInfoRow(sheet, rowIndex, "Fecha de emisión", formatDate(LocalDate.now()));
    for (ReportFilterLine filter : context.filters()) {
      rowIndex = writeExcelInfoRow(sheet, rowIndex, filter.label(), filter.value());
    }
    return rowIndex + 1;
  }

  private int writeExcelInfoRow(org.apache.poi.ss.usermodel.Sheet sheet, int rowIndex, String label, String value) {
    Row row = sheet.createRow(rowIndex);
    row.createCell(0).setCellValue(label);
    row.createCell(1).setCellValue(value);
    return rowIndex + 1;
  }

  private void writeExcelHeader(Row headerRow, XSSFWorkbook workbook, String[] headers) {
    CellStyle headerStyle = workbook.createCellStyle();
    org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
    headerFont.setBold(true);
    headerStyle.setFont(headerFont);
    headerStyle.setAlignment(HorizontalAlignment.CENTER);
    headerStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
    headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    for (int index = 0; index < headers.length; index++) {
      var cell = headerRow.createCell(index);
      cell.setCellValue(headers[index]);
      cell.setCellStyle(headerStyle);
    }
  }

  private void writeAssetExcelRow(Row excelRow, int index, AssetReportRowResponse row) {
    excelRow.createCell(0).setCellValue(index);
    excelRow.createCell(1).setCellValue(row.code());
    excelRow.createCell(2).setCellValue(row.category());
    excelRow.createCell(3).setCellValue(row.description());
    excelRow.createCell(4).setCellValue(row.location());
    excelRow.createCell(5).setCellValue(row.condition().getLabel());
    excelRow.createCell(6).setCellValue(formatDate(row.acquisitionDate()));
  }

  private void writeDecommissionedAssetExcelRow(Row excelRow, int index, AssetReportRowResponse row) {
    writeAssetExcelRow(excelRow, index, row);
    excelRow.createCell(7).setCellValue(formatInstantDate(row.decommissionedAt(), "No registrada"));
  }

  private void writeLoanExcelRow(Row excelRow, int index, LoanReportRowResponse row) {
    excelRow.createCell(0).setCellValue(index);
    excelRow.createCell(1).setCellValue(row.code());
    excelRow.createCell(2).setCellValue(row.teacherName());
    excelRow.createCell(3).setCellValue(row.assetsCount());
    excelRow.createCell(4).setCellValue(formatDate(row.loanDate()));
    excelRow.createCell(5).setCellValue(formatDate(row.dueDate()));
    excelRow.createCell(6).setCellValue(row.location());
    excelRow.createCell(7).setCellValue(row.status());
  }

  private void autosize(org.apache.poi.ss.usermodel.Sheet sheet, int columnCount) {
    for (int index = 0; index < columnCount; index++) {
      sheet.autoSizeColumn(index);
    }
  }

  private byte[] buildAssetsWord(List<AssetReportRowResponse> rows, ReportDocumentContext context) {
    try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      AssetReportSections sections = splitAssetRows(rows);
      addWordReportHeader(document, context);
      addWordInformativeData(document, context);
      addWordSection(document, "II. Introducción", context.introduction());
      addWordHeading(document, "III. Detalle del reporte");
      XWPFTable table = document.createTable(sections.activeRows().isEmpty() ? 2 : sections.activeRows().size() + 1, ASSET_HEADERS.length);
      writeWordHeader(table.getRow(0), ASSET_HEADERS);
      if (sections.activeRows().isEmpty()) {
        table.getRow(1).getCell(0).setText("No hay registros para los filtros aplicados.");
      } else {
        for (int index = 0; index < sections.activeRows().size(); index++) {
          writeAssetWordRow(table.getRow(index + 1), index + 1, sections.activeRows().get(index));
        }
      }
      if (!sections.decommissionedRows().isEmpty()) {
        addWordHeading(document, "Activos dados de baja");
        XWPFTable decommissionedTable = document.createTable(sections.decommissionedRows().size() + 1, DECOMMISSIONED_ASSET_HEADERS.length);
        writeWordHeader(decommissionedTable.getRow(0), DECOMMISSIONED_ASSET_HEADERS);
        for (int index = 0; index < sections.decommissionedRows().size(); index++) {
          writeDecommissionedAssetWordRow(decommissionedTable.getRow(index + 1), index + 1, sections.decommissionedRows().get(index));
        }
      }
      addWordClosingAndSignature(document, context);
      document.write(output);
      return output.toByteArray();
    } catch (IOException | InvalidFormatException exception) {
      throw new IllegalStateException("No se pudo generar el reporte Word.", exception);
    }
  }

  private byte[] buildPhysicalInventoryWord(PhysicalInventoryExportContext context) {
    try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      setWordLandscape(document);
      addPhysicalWordLogo(document, context.settings());
      PhysicalInventoryReportResponse report = context.report();
      addWordCenteredLine(document, report.ugelName(), true);
      addWordCenteredLine(document, report.institutionName(), true);
      addWordCenteredLine(document, report.title(), true);
      addWordCenteredLine(document, report.locationSubtitle(), true);
      addWordCenteredLine(document, "Generado por: " + report.generatedBy() + " | Fecha: " + formatDate(report.generatedAt()), false);
      XWPFTable table = document.createTable(report.rows().size() + 2, 9);
      String[] header = {"ITEM", "DESCRIPCIÓN DEL BIEN", "UBICACIÓN", "ESTADO", "", "", "MARCA", "OBSERVACIONES", "CANTIDAD"};
      String[] stateHeader = {"", "", "", "B", "R", "M", "", "", ""};
      writeWordHeader(table.getRow(0), header);
      writeWordHeader(table.getRow(1), stateHeader);
      for (int index = 0; index < report.rows().size(); index++) {
        PhysicalInventoryReportRowResponse row = report.rows().get(index);
        XWPFTableRow wordRow = table.getRow(index + 2);
        wordRow.getCell(0).setText(String.valueOf(index + 1));
        wordRow.getCell(1).setText(row.assetDescription());
        wordRow.getCell(2).setText(row.location());
        wordRow.getCell(3).setText(row.good() ? "X" : "");
        wordRow.getCell(4).setText(row.regular() ? "X" : "");
        wordRow.getCell(5).setText(row.bad() ? "X" : "");
        wordRow.getCell(6).setText(row.brand());
        wordRow.getCell(7).setText(row.observations());
        wordRow.getCell(8).setText(String.valueOf(row.quantity()));
      }
      addPhysicalWordSignature(document, report.generatedBy(), context.signature());
      document.write(output);
      return output.toByteArray();
    } catch (IOException | InvalidFormatException exception) {
      throw new IllegalStateException("No se pudo generar el inventario físico en Word.", exception);
    }
  }

  private void addPhysicalWordLogo(XWPFDocument document, InstitutionSettings settings) throws IOException, InvalidFormatException {
    if (!settings.hasLogo()) {
      return;
    }
    XWPFParagraph logoParagraph = document.createParagraph();
    logoParagraph.setAlignment(ParagraphAlignment.CENTER);
    XWPFRun logoRun = logoParagraph.createRun();
    logoRun.addPicture(
        new ByteArrayInputStream(settings.getLogoContent()),
        wordPictureType(settings.getLogoMimeType()),
        normalizeText(settings.getLogoFileName(), "institution-logo"),
        Units.toEMU(48),
        Units.toEMU(48)
    );
  }

  private void addPhysicalWordSignature(XWPFDocument document, String generatedBy, ReportSignature signature)
      throws IOException, InvalidFormatException {
    XWPFParagraph signatureParagraph = document.createParagraph();
    signatureParagraph.setAlignment(ParagraphAlignment.CENTER);
    XWPFRun run = signatureParagraph.createRun();
    for (int index = 0; index < 3; index++) {
      run.addBreak();
    }
    if (signature != null) {
      int[] dimensions = signatureDimensions(signature.content(), 160, 48);
      run.addPicture(
          new ByteArrayInputStream(signature.content()),
          XWPFDocument.PICTURE_TYPE_PNG,
          signature.fileName(),
          Units.toEMU(dimensions[0]),
          Units.toEMU(dimensions[1])
      );
      run.addBreak();
    }
    run.setText("____________________________________________");
    run.addBreak();
    run.setBold(true);
    run.setText(signatureLabel(generatedBy));
  }

  private String signatureLabel(String generatedBy) {
    String normalizedName = normalizeText(generatedBy, "");
    return normalizedName.regionMatches(true, 0, "Prof.", 0, 5)
        ? normalizedName
        : "Prof. " + normalizedName;
  }

  private int[] signatureDimensions(byte[] signatureContent, int maxWidth, int maxHeight) throws IOException {
    BufferedImage image = ImageIO.read(new ByteArrayInputStream(signatureContent));
    if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
      return new int[] { maxWidth, maxHeight };
    }
    double scale = Math.min((double) maxWidth / image.getWidth(), (double) maxHeight / image.getHeight());
    return new int[] {
        Math.max(1, (int) Math.round(image.getWidth() * scale)),
        Math.max(1, (int) Math.round(image.getHeight() * scale))
    };
  }

  private void setWordLandscape(XWPFDocument document) {
    CTSectPr section = document.getDocument().getBody().isSetSectPr()
        ? document.getDocument().getBody().getSectPr()
        : document.getDocument().getBody().addNewSectPr();
    CTPageSz pageSize = section.isSetPgSz() ? section.getPgSz() : section.addNewPgSz();
    pageSize.setOrient(STPageOrientation.LANDSCAPE);
    pageSize.setW(BigInteger.valueOf(15840));
    pageSize.setH(BigInteger.valueOf(12240));
  }

  private void addWordCenteredLine(XWPFDocument document, String value, boolean bold) {
    if (value == null || value.isBlank()) {
      return;
    }
    XWPFParagraph paragraph = document.createParagraph();
    paragraph.setAlignment(ParagraphAlignment.CENTER);
    XWPFRun run = paragraph.createRun();
    run.setBold(bold);
    run.setText(value);
  }

  private byte[] buildLoansWord(List<LoanReportRowResponse> rows, ReportDocumentContext context) {
    try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      addWordReportHeader(document, context);
      addWordInformativeData(document, context);
      addWordSection(document, "II. Introducción", context.introduction());
      addWordHeading(document, "III. Detalle del reporte");
      XWPFTable table = document.createTable(rows.isEmpty() ? 2 : rows.size() + 1, LOAN_HEADERS.length);
      writeWordHeader(table.getRow(0), LOAN_HEADERS);
      if (rows.isEmpty()) {
        table.getRow(1).getCell(0).setText("No hay registros para los filtros aplicados.");
      } else {
        for (int index = 0; index < rows.size(); index++) {
          writeLoanWordRow(table.getRow(index + 1), index + 1, rows.get(index));
        }
      }
      addWordClosingAndSignature(document, context);
      document.write(output);
      return output.toByteArray();
    } catch (IOException | InvalidFormatException exception) {
      throw new IllegalStateException("No se pudo generar el reporte Word.", exception);
    }
  }

  private void addWordReportHeader(XWPFDocument document, ReportDocumentContext context) throws IOException, InvalidFormatException {
    XWPFParagraph logoParagraph = document.createParagraph();
    logoParagraph.setAlignment(ParagraphAlignment.CENTER);
    if (context.settings().hasLogo()) {
      XWPFRun logoRun = logoParagraph.createRun();
      logoRun.addPicture(
          new ByteArrayInputStream(context.settings().getLogoContent()),
          wordPictureType(context.settings().getLogoMimeType()),
          normalizeText(context.settings().getLogoFileName(), "institution-logo"),
          Units.toEMU(48),
          Units.toEMU(48)
      );
    }

    XWPFParagraph institution = document.createParagraph();
    institution.setAlignment(ParagraphAlignment.CENTER);
    XWPFRun institutionRun = institution.createRun();
    institutionRun.setBold(true);
    institutionRun.setFontSize(11);
    institutionRun.setText(normalizeText(context.settings().getSystemName(), "Sistema de gestión de activos").toUpperCase(Locale.ROOT));

    String locationLine = buildInstitutionLocationLine(context.settings());
    if (!locationLine.isBlank()) {
      XWPFParagraph location = document.createParagraph();
      location.setAlignment(ParagraphAlignment.CENTER);
      location.createRun().setText(locationLine);
    }

    XWPFParagraph title = document.createParagraph();
    title.setAlignment(ParagraphAlignment.CENTER);
    XWPFRun titleRun = title.createRun();
    titleRun.setBold(true);
    titleRun.setFontSize(14);
    titleRun.setText(context.title());
  }

  private void addWordInformativeData(XWPFDocument document, ReportDocumentContext context) {
    addWordHeading(document, "I. Datos informativos");
    addWordLine(document, "Institución educativa: " + normalizeText(context.settings().getSystemName(), "No registrado"));
    addWordLine(document, "Ciudad: " + normalizeText(context.settings().getCity(), "No registrado"));
    addWordLine(document, "Dirección: " + normalizeText(context.settings().getAddress(), "No registrado"));
    addWordLine(document, "Fecha de emisión: " + formatDate(LocalDate.now()));
    addWordLine(document, "Emitido por: " + context.generatedBy().name());
    for (ReportFilterLine filter : context.filters()) {
      addWordLine(document, filter.label() + ": " + filter.value());
    }
  }

  private void addWordSection(XWPFDocument document, String title, String body) {
    addWordHeading(document, title);
    XWPFParagraph paragraph = document.createParagraph();
    paragraph.setAlignment(ParagraphAlignment.BOTH);
    paragraph.createRun().setText(body);
  }

  private void addWordHeading(XWPFDocument document, String value) {
    XWPFParagraph paragraph = document.createParagraph();
    XWPFRun run = paragraph.createRun();
    run.setBold(true);
    run.setFontSize(12);
    run.setText(value);
  }

  private void addWordLine(XWPFDocument document, String value) {
    XWPFParagraph paragraph = document.createParagraph();
    paragraph.createRun().setText(value);
  }

  private void writeWordHeader(XWPFTableRow headerRow, String[] headers) {
    for (int index = 0; index < headers.length; index++) {
      headerRow.getCell(index).setText(headers[index]);
    }
  }

  private void writeAssetWordRow(XWPFTableRow wordRow, int index, AssetReportRowResponse row) {
    wordRow.getCell(0).setText(String.valueOf(index));
    wordRow.getCell(1).setText(row.code());
    wordRow.getCell(2).setText(row.category());
    wordRow.getCell(3).setText(row.description());
    wordRow.getCell(4).setText(row.location());
    wordRow.getCell(5).setText(row.condition().getLabel());
    wordRow.getCell(6).setText(formatDate(row.acquisitionDate()));
  }

  private void writeDecommissionedAssetWordRow(XWPFTableRow wordRow, int index, AssetReportRowResponse row) {
    writeAssetWordRow(wordRow, index, row);
    wordRow.getCell(7).setText(formatInstantDate(row.decommissionedAt(), "No registrada"));
  }

  private void writeLoanWordRow(XWPFTableRow wordRow, int index, LoanReportRowResponse row) {
    wordRow.getCell(0).setText(String.valueOf(index));
    wordRow.getCell(1).setText(row.code());
    wordRow.getCell(2).setText(row.teacherName());
    wordRow.getCell(3).setText(String.valueOf(row.assetsCount()));
    wordRow.getCell(4).setText(formatDate(row.loanDate()));
    wordRow.getCell(5).setText(formatDate(row.dueDate()));
    wordRow.getCell(6).setText(row.location());
    wordRow.getCell(7).setText(row.status());
  }

  private void addWordClosingAndSignature(XWPFDocument document, ReportDocumentContext context) {
    XWPFParagraph closing = document.createParagraph();
    closing.createRun().setText(context.closingText());
    XWPFParagraph salutation = document.createParagraph();
    salutation.setAlignment(ParagraphAlignment.CENTER);
    salutation.createRun().setText("Atentamente,");

    XWPFParagraph signature = document.createParagraph();
    signature.setAlignment(ParagraphAlignment.CENTER);
    XWPFRun run = signature.createRun();
    run.setText("______________________________");
    run.addBreak();
    run.setBold(true);
    run.setText(context.generatedBy().name());
  }

  private int wordPictureType(String mimeType) {
    if ("image/png".equalsIgnoreCase(mimeType)) {
      return XWPFDocument.PICTURE_TYPE_PNG;
    }
    if ("image/gif".equalsIgnoreCase(mimeType)) {
      return XWPFDocument.PICTURE_TYPE_GIF;
    }
    return XWPFDocument.PICTURE_TYPE_JPEG;
  }

  private void ensureValidDateRange(LocalDate startDate, LocalDate endDate) {
    if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
      throw new BadRequestException("La fecha inicial no puede ser posterior a la fecha final.");
    }
  }

  private boolean matchesDateRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
    if (date == null) {
      return false;
    }
    return (startDate == null || !date.isBefore(startDate)) && (endDate == null || !date.isAfter(endDate));
  }

  private boolean matchesLoanSearch(Loan loan, String search) {
    if (search == null) {
      return true;
    }
    return List.of(
            loan.getCode(),
            loan.getTeacherNameSnapshot(),
            loan.getTeacherDniSnapshot(),
            loan.getDestinationNameSnapshot()
        ).stream()
        .filter(value -> value != null && !value.isBlank())
        .map(value -> value.toLowerCase(Locale.ROOT))
        .anyMatch(value -> value.contains(search));
  }

  private String formatDate(LocalDate date) {
    return date == null ? "" : DATE_FORMATTER.format(date);
  }

  private String formatInstantDate(Instant date, String fallback) {
    return date == null ? fallback : DATE_FORMATTER.format(date.atZone(java.time.ZoneOffset.UTC).toLocalDate());
  }

  private String formatDateRange(LocalDate startDate, LocalDate endDate) {
    if (startDate == null && endDate == null) {
      return "Todos";
    }
    if (startDate != null && endDate != null) {
      return formatDate(startDate) + " al " + formatDate(endDate);
    }
    if (startDate != null) {
      return "Desde " + formatDate(startDate);
    }
    return "Hasta " + formatDate(endDate);
  }

  private String buildInstitutionLocationLine(InstitutionSettings settings) {
    if (settings == null) {
      return "";
    }
    return Stream.of(settings.getAddress(), settings.getCity())
        .filter(value -> value != null && !value.isBlank())
        .map(String::trim)
        .reduce((address, city) -> address + " - " + city)
        .orElse("");
  }

  private AssetReportSections splitAssetRows(List<AssetReportRowResponse> rows) {
    List<AssetReportRowResponse> activeRows = new ArrayList<>();
    List<AssetReportRowResponse> decommissionedRows = new ArrayList<>();
    for (AssetReportRowResponse row : rows) {
      if (row.condition() == AssetCondition.DADO_DE_BAJA) {
        decommissionedRows.add(row);
      } else {
        activeRows.add(row);
      }
    }
    return new AssetReportSections(activeRows, decommissionedRows);
  }

  private PdfPTable buildAssetPdfTable(List<AssetReportRowResponse> rows, String[] headers, float[] widths) throws DocumentException {
    PdfPTable table = new PdfPTable(headers.length);
    table.setWidthPercentage(100);
    table.setWidths(widths);
    addPdfHeader(table, headers);
    for (int index = 0; index < rows.size(); index++) {
      addAssetPdfRow(table, index + 1, rows.get(index));
    }
    addEmptyPdfRowIfNeeded(table, headers.length, rows.isEmpty());
    return table;
  }

  private PdfPTable buildDecommissionedAssetPdfTable(List<AssetReportRowResponse> rows) throws DocumentException {
    PdfPTable table = new PdfPTable(DECOMMISSIONED_ASSET_HEADERS.length);
    table.setWidthPercentage(100);
    table.setWidths(new float[] { 0.55f, 1.25f, 1.3f, 2.2f, 1.35f, 1.0f, 0.95f, 0.95f });
    addPdfHeader(table, DECOMMISSIONED_ASSET_HEADERS);
    for (int index = 0; index < rows.size(); index++) {
      addDecommissionedAssetPdfRow(table, index + 1, rows.get(index));
    }
    return table;
  }

  private String normalizeText(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private record ReportDocumentContext(
      String title,
      String introduction,
      String closingText,
      InstitutionSettings settings,
      ReportGeneratedBy generatedBy,
      List<ReportFilterLine> filters
  ) {}

  private record ReportGeneratedBy(String name, String email, String role) {}

  private record PhysicalInventoryExportContext(
      PhysicalInventoryReportResponse report,
      InstitutionSettings settings,
      ReportSignature signature
  ) {}

  private record ReportSignature(byte[] content, String fileName) {}

  private record AssetReportSections(
      List<AssetReportRowResponse> activeRows,
      List<AssetReportRowResponse> decommissionedRows
  ) {}

  private record ReportFilterLine(String label, String value) {}
}
