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
import com.sigae.api.model.dto.ReportExportFile;
import com.sigae.api.model.dto.ReportExportFormat;
import com.sigae.api.model.entity.Asset;
import com.sigae.api.model.entity.InstitutionSettings;
import com.sigae.api.model.entity.Loan;
import com.sigae.api.model.entity.User;
import com.sigae.api.repository.AssetRepository;
import com.sigae.api.repository.CategoryRepository;
import com.sigae.api.repository.LoanRepository;
import com.sigae.api.repository.LocationRepository;
import com.sigae.api.repository.UserRepository;
import com.sigae.api.security.AuthenticatedUser;
import jakarta.persistence.criteria.Predicate;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    List<AssetReportRowResponse> rows = listAssetRows(categoryId, locationId, startDate, endDate);
    ReportDocumentContext context = buildAssetContext(categoryId, locationId, startDate, endDate, authenticatedUser);
    byte[] content = switch (format) {
      case PDF -> buildAssetsPdf(rows, context);
      case EXCEL -> buildAssetsExcel(rows, context);
      case WORD -> buildAssetsWord(rows, context);
    };

    return new ReportExportFile(
        "reporte-activos-%s.%s".formatted(LocalDate.now(), format.extension()),
        format.contentType(),
        content
    );
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
      Document document = new Document(PageSize.A4.rotate(), 36, 36, 32, 36);
      PdfWriter.getInstance(document, output);
      document.open();
      addPdfReportHeader(document, context);
      addPdfInformativeData(document, context);
      addPdfSection(document, "II. Introducción", context.introduction());
      addPdfTitle(document, "III. Detalle del reporte", 12, Element.ALIGN_LEFT, 8);
      PdfPTable table = new PdfPTable(ASSET_HEADERS.length);
      table.setWidthPercentage(100);
      table.setWidths(new float[] { 0.6f, 1.35f, 1.4f, 2.4f, 1.45f, 1.1f, 1.0f });
      addPdfHeader(table, ASSET_HEADERS);
      for (int index = 0; index < rows.size(); index++) {
        addAssetPdfRow(table, index + 1, rows.get(index));
      }
      addEmptyPdfRowIfNeeded(table, ASSET_HEADERS.length, rows.isEmpty());
      document.add(table);
      addPdfClosingAndSignature(document, context);
      document.close();
      return output.toByteArray();
    } catch (Exception exception) {
      throw new IllegalStateException("No se pudo generar el reporte PDF.", exception);
    }
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
    addPdfInfoRow(table, "Rol", normalizeText(context.generatedBy().role(), "No registrado"));
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
    PdfPCell detail = pdfCell(buildGeneratedByDetail(context.generatedBy()), FontFactory.getFont(FontFactory.HELVETICA, 8), Element.ALIGN_CENTER);
    detail.setBorder(PdfPCell.NO_BORDER);
    signature.addCell(detail);
    document.add(signature);
  }

  private byte[] buildAssetsExcel(List<AssetReportRowResponse> rows, ReportDocumentContext context) {
    try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      var sheet = workbook.createSheet("Activos");
      int rowIndex = writeExcelMetadata(sheet, workbook, context);
      writeExcelHeader(sheet.createRow(rowIndex++), workbook, ASSET_HEADERS);
      for (int index = 0; index < rows.size(); index++) {
        Row row = sheet.createRow(rowIndex++);
        writeAssetExcelRow(row, index + 1, rows.get(index));
      }
      autosize(sheet, ASSET_HEADERS.length);
      workbook.write(output);
      return output.toByteArray();
    } catch (IOException exception) {
      throw new IllegalStateException("No se pudo generar el reporte Excel.", exception);
    }
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
      addWordReportHeader(document, context);
      addWordInformativeData(document, context);
      addWordSection(document, "II. Introducción", context.introduction());
      addWordHeading(document, "III. Detalle del reporte");
      XWPFTable table = document.createTable(rows.isEmpty() ? 2 : rows.size() + 1, ASSET_HEADERS.length);
      writeWordHeader(table.getRow(0), ASSET_HEADERS);
      if (rows.isEmpty()) {
        table.getRow(1).getCell(0).setText("No hay registros para los filtros aplicados.");
      } else {
        for (int index = 0; index < rows.size(); index++) {
          writeAssetWordRow(table.getRow(index + 1), index + 1, rows.get(index));
        }
      }
      addWordClosingAndSignature(document, context);
      document.write(output);
      return output.toByteArray();
    } catch (IOException | InvalidFormatException exception) {
      throw new IllegalStateException("No se pudo generar el reporte Word.", exception);
    }
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
    addWordLine(document, "Rol: " + normalizeText(context.generatedBy().role(), "No registrado"));
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
    run.addBreak();
    run.setBold(false);
    run.setText(buildGeneratedByDetail(context.generatedBy()));
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

  private String buildGeneratedByDetail(ReportGeneratedBy generatedBy) {
    String email = generatedBy.email() == null ? "" : generatedBy.email().trim();
    String role = generatedBy.role() == null ? "" : generatedBy.role().trim();
    if (email.isBlank() && role.isBlank()) {
      return "";
    }
    if (email.isBlank()) {
      return role;
    }
    if (role.isBlank()) {
      return email;
    }
    return role + " · " + email;
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

  private record ReportFilterLine(String label, String value) {}
}
