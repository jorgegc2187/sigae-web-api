package com.sigae.api.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
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
import com.sigae.api.model.entity.Loan;
import com.sigae.api.repository.AssetRepository;
import com.sigae.api.repository.LoanRepository;
import jakarta.persistence.criteria.Predicate;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
      "Código",
      "Descripción",
      "Categoría",
      "Ubicación",
      "Estado",
      "Fecha alta"
  };
  private static final String[] LOAN_HEADERS = {
      "ID Préstamo",
      "Docente",
      "Cant. Activos",
      "Fecha préstamo",
      "Fecha límite",
      "Ubicación",
      "Estado"
  };

  private final AssetRepository assetRepository;
  private final LoanRepository loanRepository;
  private final LoanService loanService;

  public ReportService(
      AssetRepository assetRepository,
      LoanRepository loanRepository,
      LoanService loanService
  ) {
    this.assetRepository = assetRepository;
    this.loanRepository = loanRepository;
    this.loanService = loanService;
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
      ReportExportFormat format
  ) {
    List<AssetReportRowResponse> rows = listAssetRows(categoryId, locationId, startDate, endDate);
    byte[] content = switch (format) {
      case PDF -> buildPdf(rows);
      case EXCEL -> buildExcel(rows);
      case WORD -> buildWord(rows);
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
    String normalizedSearch = search == null || search.isBlank() ? null : search.trim().toLowerCase();
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
      ReportExportFormat format
  ) {
    List<LoanReportRowResponse> rows = listLoanRows(search, locationId, startDate, endDate);
    byte[] content = switch (format) {
      case PDF -> buildLoansPdf(rows);
      case EXCEL -> buildLoansExcel(rows);
      case WORD -> buildLoansWord(rows);
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
        .map(value -> value.toLowerCase(java.util.Locale.ROOT))
        .anyMatch(value -> value.contains(search));
  }

  private byte[] buildPdf(List<AssetReportRowResponse> rows) {
    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
      PdfWriter.getInstance(document, output);
      document.open();

      Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
      Paragraph title = new Paragraph("Reporte de Activos", titleFont);
      title.setAlignment(Element.ALIGN_LEFT);
      title.setSpacingAfter(16);
      document.add(title);

      PdfPTable table = new PdfPTable(ASSET_HEADERS.length);
      table.setWidthPercentage(100);
      addPdfHeader(table);
      rows.forEach(row -> addPdfRow(table, row));
      document.add(table);
      document.close();
      return output.toByteArray();
    } catch (DocumentException | IOException exception) {
      throw new IllegalStateException("No se pudo generar el reporte PDF.", exception);
    }
  }

  private void addPdfHeader(PdfPTable table) {
    Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    for (String header : ASSET_HEADERS) {
      PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
      cell.setPadding(6);
      table.addCell(cell);
    }
  }

  private void addPdfRow(PdfPTable table, AssetReportRowResponse row) {
    table.addCell(pdfCell(row.code()));
    table.addCell(pdfCell(row.description()));
    table.addCell(pdfCell(row.category()));
    table.addCell(pdfCell(row.location()));
    table.addCell(pdfCell(row.condition().getLabel()));
    table.addCell(pdfCell(formatDate(row.acquisitionDate())));
  }

  private PdfPCell pdfCell(String value) {
    PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value));
    cell.setPadding(5);
    return cell;
  }

  private byte[] buildExcel(List<AssetReportRowResponse> rows) {
    try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      var sheet = workbook.createSheet("Activos");
      CellStyle headerStyle = workbook.createCellStyle();
      org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
      headerFont.setBold(true);
      headerStyle.setFont(headerFont);

      Row headerRow = sheet.createRow(0);
      for (int index = 0; index < ASSET_HEADERS.length; index++) {
        var cell = headerRow.createCell(index);
        cell.setCellValue(ASSET_HEADERS[index]);
        cell.setCellStyle(headerStyle);
      }

      for (int index = 0; index < rows.size(); index++) {
        Row row = sheet.createRow(index + 1);
        writeExcelRow(row, rows.get(index));
      }

      for (int index = 0; index < ASSET_HEADERS.length; index++) {
        sheet.autoSizeColumn(index);
      }

      workbook.write(output);
      return output.toByteArray();
    } catch (IOException exception) {
      throw new IllegalStateException("No se pudo generar el reporte Excel.", exception);
    }
  }

  private void writeExcelRow(Row excelRow, AssetReportRowResponse row) {
    excelRow.createCell(0).setCellValue(row.code());
    excelRow.createCell(1).setCellValue(row.description());
    excelRow.createCell(2).setCellValue(row.category());
    excelRow.createCell(3).setCellValue(row.location());
    excelRow.createCell(4).setCellValue(row.condition().getLabel());
    excelRow.createCell(5).setCellValue(formatDate(row.acquisitionDate()));
  }

  private byte[] buildWord(List<AssetReportRowResponse> rows) {
    try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      XWPFParagraph title = document.createParagraph();
      title.setAlignment(ParagraphAlignment.LEFT);
      XWPFRun titleRun = title.createRun();
      titleRun.setBold(true);
      titleRun.setFontSize(16);
      titleRun.setText("Reporte de Activos");

      XWPFTable table = document.createTable(rows.size() + 1, ASSET_HEADERS.length);
      XWPFTableRow headerRow = table.getRow(0);
      for (int index = 0; index < ASSET_HEADERS.length; index++) {
        headerRow.getCell(index).setText(ASSET_HEADERS[index]);
      }

      for (int index = 0; index < rows.size(); index++) {
        writeWordRow(table.getRow(index + 1), rows.get(index));
      }

      document.write(output);
      return output.toByteArray();
    } catch (IOException exception) {
      throw new IllegalStateException("No se pudo generar el reporte Word.", exception);
    }
  }

  private void writeWordRow(XWPFTableRow wordRow, AssetReportRowResponse row) {
    wordRow.getCell(0).setText(row.code());
    wordRow.getCell(1).setText(row.description());
    wordRow.getCell(2).setText(row.category());
    wordRow.getCell(3).setText(row.location());
    wordRow.getCell(4).setText(row.condition().getLabel());
    wordRow.getCell(5).setText(formatDate(row.acquisitionDate()));
  }

  private String formatDate(LocalDate date) {
    return date == null ? "" : DATE_FORMATTER.format(date);
  }

  private byte[] buildLoansPdf(List<LoanReportRowResponse> rows) {
    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
      PdfWriter.getInstance(document, output);
      document.open();
      Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
      Paragraph title = new Paragraph("Reporte de Préstamos", titleFont);
      title.setAlignment(Element.ALIGN_LEFT);
      title.setSpacingAfter(16);
      document.add(title);
      PdfPTable table = new PdfPTable(LOAN_HEADERS.length);
      table.setWidthPercentage(100);
      addPdfHeader(table, LOAN_HEADERS);
      rows.forEach(row -> addLoanPdfRow(table, row));
      document.add(table);
      document.close();
      return output.toByteArray();
    } catch (DocumentException | IOException exception) {
      throw new IllegalStateException("No se pudo generar el reporte PDF.", exception);
    }
  }

  private void addPdfHeader(PdfPTable table, String[] headers) {
    Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    for (String header : headers) {
      PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
      cell.setPadding(6);
      table.addCell(cell);
    }
  }

  private void addLoanPdfRow(PdfPTable table, LoanReportRowResponse row) {
    table.addCell(pdfCell(row.code()));
    table.addCell(pdfCell(row.teacherName()));
    table.addCell(pdfCell(String.valueOf(row.assetsCount())));
    table.addCell(pdfCell(formatDate(row.loanDate())));
    table.addCell(pdfCell(formatDate(row.dueDate())));
    table.addCell(pdfCell(row.location()));
    table.addCell(pdfCell(row.status()));
  }

  private byte[] buildLoansExcel(List<LoanReportRowResponse> rows) {
    try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      var sheet = workbook.createSheet("Préstamos");
      CellStyle headerStyle = workbook.createCellStyle();
      org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
      headerFont.setBold(true);
      headerStyle.setFont(headerFont);
      Row headerRow = sheet.createRow(0);
      for (int index = 0; index < LOAN_HEADERS.length; index++) {
        var cell = headerRow.createCell(index);
        cell.setCellValue(LOAN_HEADERS[index]);
        cell.setCellStyle(headerStyle);
      }
      for (int index = 0; index < rows.size(); index++) {
        Row row = sheet.createRow(index + 1);
        writeLoanExcelRow(row, rows.get(index));
      }
      for (int index = 0; index < LOAN_HEADERS.length; index++) {
        sheet.autoSizeColumn(index);
      }
      workbook.write(output);
      return output.toByteArray();
    } catch (IOException exception) {
      throw new IllegalStateException("No se pudo generar el reporte Excel.", exception);
    }
  }

  private void writeLoanExcelRow(Row excelRow, LoanReportRowResponse row) {
    excelRow.createCell(0).setCellValue(row.code());
    excelRow.createCell(1).setCellValue(row.teacherName());
    excelRow.createCell(2).setCellValue(row.assetsCount());
    excelRow.createCell(3).setCellValue(formatDate(row.loanDate()));
    excelRow.createCell(4).setCellValue(formatDate(row.dueDate()));
    excelRow.createCell(5).setCellValue(row.location());
    excelRow.createCell(6).setCellValue(row.status());
  }

  private byte[] buildLoansWord(List<LoanReportRowResponse> rows) {
    try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      XWPFParagraph title = document.createParagraph();
      title.setAlignment(ParagraphAlignment.LEFT);
      XWPFRun titleRun = title.createRun();
      titleRun.setBold(true);
      titleRun.setFontSize(16);
      titleRun.setText("Reporte de Préstamos");
      XWPFTable table = document.createTable(rows.size() + 1, LOAN_HEADERS.length);
      XWPFTableRow headerRow = table.getRow(0);
      for (int index = 0; index < LOAN_HEADERS.length; index++) {
        headerRow.getCell(index).setText(LOAN_HEADERS[index]);
      }
      for (int index = 0; index < rows.size(); index++) {
        writeLoanWordRow(table.getRow(index + 1), rows.get(index));
      }
      document.write(output);
      return output.toByteArray();
    } catch (IOException exception) {
      throw new IllegalStateException("No se pudo generar el reporte Word.", exception);
    }
  }

  private void writeLoanWordRow(XWPFTableRow wordRow, LoanReportRowResponse row) {
    wordRow.getCell(0).setText(row.code());
    wordRow.getCell(1).setText(row.teacherName());
    wordRow.getCell(2).setText(String.valueOf(row.assetsCount()));
    wordRow.getCell(3).setText(formatDate(row.loanDate()));
    wordRow.getCell(4).setText(formatDate(row.dueDate()));
    wordRow.getCell(5).setText(row.location());
    wordRow.getCell(6).setText(row.status());
  }
}
