package com.sigae.api.service;

import com.sigae.api.model.entity.Asset;
import com.sigae.api.model.entity.AssetAttributeDefinition;
import com.sigae.api.model.entity.AssetAttributeValue;
import com.sigae.api.model.entity.AssetCondition;
import com.sigae.api.model.entity.AssetTraceability;
import com.sigae.api.model.entity.AssetType;
import com.sigae.api.model.entity.CatalogStatus;
import com.sigae.api.model.entity.Category;
import com.sigae.api.model.entity.Location;
import com.sigae.api.model.entity.Supplier;
import com.sigae.api.model.entity.Teacher;
import com.sigae.api.model.entity.TraceabilityEventType;
import com.sigae.api.repository.AssetRepository;
import com.sigae.api.repository.AssetTraceabilityRepository;
import com.sigae.api.repository.CategoryRepository;
import com.sigae.api.repository.LocationRepository;
import com.sigae.api.repository.SupplierRepository;
import com.sigae.api.repository.TeacherRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DevInventorySeedService {

  private final CategoryRepository categoryRepository;
  private final LocationRepository locationRepository;
  private final SupplierRepository supplierRepository;
  private final TeacherRepository teacherRepository;
  private final AssetRepository assetRepository;
  private final AssetTraceabilityRepository assetTraceabilityRepository;

  public DevInventorySeedService(
      CategoryRepository categoryRepository,
      LocationRepository locationRepository,
      SupplierRepository supplierRepository,
      TeacherRepository teacherRepository,
      AssetRepository assetRepository,
      AssetTraceabilityRepository assetTraceabilityRepository
  ) {
    this.categoryRepository = categoryRepository;
    this.locationRepository = locationRepository;
    this.supplierRepository = supplierRepository;
    this.teacherRepository = teacherRepository;
    this.assetRepository = assetRepository;
    this.assetTraceabilityRepository = assetTraceabilityRepository;
  }

  public void seed() {
    Map<String, Category> categoriesByName = seedCategories().stream()
        .collect(Collectors.toMap(Category::getName, category -> category, (left, right) -> right, LinkedHashMap::new));
    Map<String, Location> locationsByName = seedLocations().stream()
        .collect(Collectors.toMap(Location::getName, location -> location, (left, right) -> right, LinkedHashMap::new));
    seedTeachers();
    List<Supplier> suppliers = seedSuppliers();

    List<SeedAsset> assets = seedAssets();
    for (int index = 0; index < assets.size(); index++) {
      upsertAsset(assets.get(index), categoriesByName, locationsByName, suppliers.get(index % suppliers.size()), index);
    }
  }

  private List<Category> seedCategories() {
    List<SeedCategory> definitions = List.of(
        new SeedCategory("Tecnología", "devices", List.of(
            new SeedType("Laptop", "laptop_mac", List.of(
                new SeedAttribute("Marca", "Fabricante del equipo", true),
                new SeedAttribute("Modelo", "Modelo comercial del equipo", true),
                new SeedAttribute("Procesador", "CPU instalada", false),
                new SeedAttribute("RAM (GB)", "Capacidad de memoria RAM", false)
            )),
            new SeedType("Desktop", "desktop_windows", List.of(
                new SeedAttribute("Marca", "Fabricante del equipo", true),
                new SeedAttribute("Modelo", "Modelo del CPU o all-in-one", true),
                new SeedAttribute("Almacenamiento", "Capacidad del disco principal", false)
            )),
            new SeedType("Proyector", "present_to_all", List.of(
                new SeedAttribute("Marca", "Fabricante del proyector", true),
                new SeedAttribute("Resolución", "Resolución nativa", true),
                new SeedAttribute("Lumens", "Brillo en ANSI lumens", false)
            )),
            new SeedType("Router", "router", List.of(
                new SeedAttribute("Marca", "Fabricante del router", true),
                new SeedAttribute("Estándar Wi-Fi", "Estándar inalámbrico soportado", true),
                new SeedAttribute("Puertos LAN", "Cantidad de puertos de red", false)
            )),
            new SeedType("Webcam", "videocam", List.of(
                new SeedAttribute("Marca", "Fabricante de la webcam", true),
                new SeedAttribute("Resolución", "Resolución de captura", true),
                new SeedAttribute("Micrófono integrado", "Disponibilidad de micrófono", false)
            )),
            new SeedType("Impresora", "print", List.of(
                new SeedAttribute("Marca", "Fabricante de la impresora", true),
                new SeedAttribute("Tecnología", "Láser, tinta u otra tecnología", true),
                new SeedAttribute("Conectividad", "USB, red o Wi-Fi", false)
            )),
            new SeedType("Tablet", "tablet_mac", List.of(
                new SeedAttribute("Marca", "Fabricante de la tablet", true),
                new SeedAttribute("Modelo", "Modelo comercial", true),
                new SeedAttribute("Almacenamiento", "Capacidad interna", false)
            )),
            new SeedType("Monitor", "monitor", List.of(
                new SeedAttribute("Marca", "Fabricante del monitor", true),
                new SeedAttribute("Tamaño", "Medida en pulgadas", true),
                new SeedAttribute("Resolución", "Resolución máxima", false)
            )),
            new SeedType("Micrófono", "mic_external_on", List.of(
                new SeedAttribute("Marca", "Fabricante del micrófono", true),
                new SeedAttribute("Tipo", "De mano, lavalier o headset", true),
                new SeedAttribute("Frecuencia", "Rango de operación", false)
            )),
            new SeedType("Cable HDMI", "settings_input_hdmi", List.of(
                new SeedAttribute("Longitud", "Longitud del cable en metros", true),
                new SeedAttribute("Versión", "Versión del estándar HDMI", false),
                new SeedAttribute("Estado físico", "Estado físico del accesorio", false)
            )),
            new SeedType("Puntero Láser", "ads_click", List.of(
                new SeedAttribute("Marca", "Fabricante del puntero", true),
                new SeedAttribute("Alcance", "Distancia máxima de proyección", false),
                new SeedAttribute("Alimentación", "Tipo de batería o carga", false)
            ))
        )),
        new SeedCategory("Mobiliario", "chair", List.of(
            new SeedType("Escritorio", "table_restaurant", List.of(
                new SeedAttribute("Material", "Material principal del escritorio", true),
                new SeedAttribute("Dimensiones", "Medidas aproximadas del escritorio", false),
                new SeedAttribute("Acabado", "Color o acabado principal", false)
            )),
            new SeedType("Silla", "chair", List.of(
                new SeedAttribute("Material", "Material estructural o de tapizado", true),
                new SeedAttribute("Tipo", "Fija, ergonómica o giratoria", true),
                new SeedAttribute("Ajustable", "Capacidad de ajuste de altura o respaldo", false)
            )),
            new SeedType("Archivador", "inventory_2", List.of(
                new SeedAttribute("Número de gavetas", "Cantidad de compartimentos", true),
                new SeedAttribute("Cerradura", "Disponibilidad de llave o sistema de bloqueo", false),
                new SeedAttribute("Material", "Material principal del archivador", false)
            )),
            new SeedType("Estante", "inventory_2", List.of(
                new SeedAttribute("Niveles", "Cantidad de niveles o repisas", true),
                new SeedAttribute("Capacidad de carga", "Peso máximo por nivel", false),
                new SeedAttribute("Material", "Material estructural", false)
            ))
        )),
        new SeedCategory("Laboratorio", "science", List.of(
            new SeedType("Microscopio", "biotech", List.of(
                new SeedAttribute("Marca", "Fabricante del microscopio", true),
                new SeedAttribute("Aumento", "Capacidad máxima de aumento", true),
                new SeedAttribute("Iluminación", "Tipo de iluminación incorporada", false)
            )),
            new SeedType("Balanza Digital", "scale", List.of(
                new SeedAttribute("Marca", "Fabricante de la balanza", true),
                new SeedAttribute("Capacidad", "Peso máximo soportado", true),
                new SeedAttribute("Precisión", "Precisión o sensibilidad del equipo", false)
            )),
            new SeedType("Kit de Química", "science", List.of(
                new SeedAttribute("Componentes", "Lista o cantidad de elementos incluidos", true),
                new SeedAttribute("Nivel", "Nivel educativo o de complejidad", false),
                new SeedAttribute("Implementos de seguridad", "Equipos o materiales de seguridad incluidos", false)
            )),
            new SeedType("Fuente de Poder", "power", List.of(
                new SeedAttribute("Rango de voltaje", "Voltaje de salida configurable", true),
                new SeedAttribute("Corriente máxima", "Corriente máxima soportada", false),
                new SeedAttribute("Pantalla digital", "Presencia de display de lectura", false)
            ))
        )),
        new SeedCategory("Deportes", "sports_soccer", List.of(
            new SeedType("Balón", "sports_soccer", List.of(
                new SeedAttribute("Disciplina", "Deporte o disciplina asociada", true),
                new SeedAttribute("Tamaño", "Tamaño o numeración oficial", true),
                new SeedAttribute("Material", "Material de fabricación", false)
            )),
            new SeedType("Cono", "sports", List.of(
                new SeedAttribute("Altura", "Altura del cono en centímetros", true),
                new SeedAttribute("Color", "Color predominante del set", false),
                new SeedAttribute("Cantidad por set", "Número de conos incluidos", false)
            )),
            new SeedType("Colchoneta", "fitness_center", List.of(
                new SeedAttribute("Dimensiones", "Largo y ancho de la colchoneta", true),
                new SeedAttribute("Espesor", "Espesor aproximado de la colchoneta", false),
                new SeedAttribute("Material", "Material de recubrimiento", false)
            )),
            new SeedType("Red", "sports_volleyball", List.of(
                new SeedAttribute("Disciplina", "Uso principal de la red", true),
                new SeedAttribute("Longitud", "Medida total de la red", false),
                new SeedAttribute("Material", "Material de fabricación", false)
            ))
        ))
    );

    List<Category> categories = new ArrayList<>();

    for (SeedCategory definition : definitions) {
      Category category = categoryRepository.findByNameIgnoreCase(definition.name())
          .orElseGet(() -> new Category(definition.name(), definition.icon()));
      category.setIcon(definition.icon());

      Map<String, AssetType> typesByName = category.getTypes().stream()
          .collect(Collectors.toMap(type -> type.getName().toLowerCase(), type -> type, (left, right) -> right, LinkedHashMap::new));

      for (SeedType typeDefinition : definition.types()) {
        AssetType type = typesByName.get(typeDefinition.name().toLowerCase());
        if (type == null) {
          type = new AssetType(typeDefinition.name(), typeDefinition.icon());
          category.addType(type);
        } else {
          type.setName(typeDefinition.name());
          type.setIcon(typeDefinition.icon());
        }

        Map<String, AssetAttributeDefinition> attributesByName = type.getAttributes().stream()
            .collect(Collectors.toMap(attribute -> attribute.getName().toLowerCase(), attribute -> attribute, (left, right) -> right, LinkedHashMap::new));

        for (SeedAttribute attributeDefinition : typeDefinition.attributes()) {
          AssetAttributeDefinition attribute = attributesByName.get(attributeDefinition.name().toLowerCase());
          if (attribute == null) {
            attribute = new AssetAttributeDefinition(
                attributeDefinition.name(),
                attributeDefinition.description(),
                attributeDefinition.required()
            );
            attribute.setAssetType(type);
            type.getAttributes().add(attribute);
          } else {
            attribute.setName(attributeDefinition.name());
            attribute.setDescription(attributeDefinition.description());
            attribute.setRequired(attributeDefinition.required());
          }
        }
      }

      categories.add(categoryRepository.save(category));
    }

    return categories;
  }

  private List<Location> seedLocations() {
    List<SeedLocation> definitions = List.of(
        new SeedLocation("Aula 101 - Pabellón A", "Ubicación operativa para equipos y mobiliario del aula 101."),
        new SeedLocation("Laboratorio de Cómputo", "Espacio principal para equipos de tecnología y conectividad."),
        new SeedLocation("Auditorio Principal", "Ubicación para equipos audiovisuales y accesorios de exposición."),
        new SeedLocation("Sala de Profesores", "Ubicación administrativa para mobiliario y periféricos."),
        new SeedLocation("Laboratorio de Ciencias", "Espacio para equipos de laboratorio y materiales científicos."),
        new SeedLocation("Almacén Deportivo", "Zona de resguardo para implementos y materiales deportivos.")
    );

    List<Location> locations = new ArrayList<>();
    for (SeedLocation definition : definitions) {
      Location location = locationRepository.findByNameIgnoreCase(definition.name())
          .orElseGet(() -> new Location(definition.name(), definition.description(), CatalogStatus.ACTIVE));
      location.setDescription(definition.description());
      location.setStatus(CatalogStatus.ACTIVE);
      locations.add(locationRepository.save(location));
    }
    return locations;
  }

  private List<Supplier> seedSuppliers() {
    List<SeedSupplier> definitions = List.of(
        new SeedSupplier("TecnoEdu Perú", "20604578912", "ventas@tecnoedu.pe", "987654321", "Av. Tecnológica 150, Lima"),
        new SeedSupplier("Mobiliario Escolar SAC", "20555888991", "contacto@mobiliarioescolar.pe", "976543210", "Jr. Carpinteros 422, Lima"),
        new SeedSupplier("LabPro Equipos", "20447766123", "soporte@labpro.pe", "965432109", "Parque Industrial Mz B Lt 8, Lima")
    );

    List<Supplier> suppliers = new ArrayList<>();
    for (SeedSupplier definition : definitions) {
      Supplier supplier = supplierRepository.findByRuc(definition.ruc())
          .or(() -> supplierRepository.findByNameIgnoreCase(definition.name()))
          .orElseGet(() -> new Supplier(
              definition.name(),
              definition.ruc(),
              definition.email(),
              definition.phone(),
              definition.address(),
              CatalogStatus.ACTIVE
          ));
      supplier.setName(definition.name());
      supplier.setRuc(definition.ruc());
      supplier.setEmail(definition.email());
      supplier.setPhone(definition.phone());
      supplier.setAddress(definition.address());
      supplier.setStatus(CatalogStatus.ACTIVE);
      suppliers.add(supplierRepository.save(supplier));
    }
    return suppliers;
  }

  private List<Teacher> seedTeachers() {
    List<SeedTeacher> definitions = List.of(
        new SeedTeacher("45678912", "Alejandro Cárdenas", "Matemáticas y Física", "a.cardenas@colegio.edu.pe", "+51 987 654 321"),
        new SeedTeacher("70123456", "Maria Rodriguez", "Comunicación e Idiomas", "m.rodriguez@colegio.edu.pe", "+51 912 345 678"),
        new SeedTeacher("12345678", "Jorge Sánchez", "Ciencia, Tecnología y Ambiente", "j.sanchez@colegio.edu.pe", "+51 955 443 322"),
        new SeedTeacher("09876543", "Elena Paredes", "Ciencias Sociales", "e.paredes@colegio.edu.pe", "+51 944 332 211"),
        new SeedTeacher("21436587", "Roberto Mendoza", "Educación Física", "r.mendoza@colegio.edu.pe", "+51 966 778 899"),
        new SeedTeacher("87654321", "Sofia Torres", "Arte y Cultura", "s.torres@colegio.edu.pe", "+51 922 110 099")
    );

    List<Teacher> teachers = new ArrayList<>();
    for (SeedTeacher definition : definitions) {
      Teacher teacher = teacherRepository.findByDni(definition.dni())
          .orElseGet(() -> new Teacher(
              definition.dni(),
              definition.fullName(),
              definition.specialty(),
              definition.email(),
              definition.phone(),
              CatalogStatus.ACTIVE
          ));
      teacher.setDni(definition.dni());
      teacher.setFullName(definition.fullName());
      teacher.setSpecialty(definition.specialty());
      teacher.setEmail(definition.email());
      teacher.setPhone(definition.phone());
      teacher.setStatus(CatalogStatus.ACTIVE);
      teachers.add(teacherRepository.save(teacher));
    }
    return teachers;
  }

  private void upsertAsset(
      SeedAsset definition,
      Map<String, Category> categoriesByName,
      Map<String, Location> locationsByName,
      Supplier supplier,
      int index
  ) {
    Category category = categoriesByName.get(definition.categoryName());
    AssetType assetType = findType(category, definition.typeName());
    Location location = locationsByName.get(definition.locationName());

    Asset asset = assetRepository.findByCodeIgnoreCase(definition.code())
        .orElseGet(() -> new Asset(
            definition.code(),
            definition.name(),
            assetType,
            location,
            supplier,
            AssetCondition.fromValue(definition.conditionLabel())
        ));

    asset.setName(definition.name());
    asset.setAssetType(assetType);
    asset.setLocation(location);
    asset.setSupplier(supplier);
    asset.setCondition(AssetCondition.fromValue(definition.conditionLabel()));
    asset.setSerialNumber(definition.serialNumber());
    asset.setBarcode("BC-" + definition.code());
    asset.setAcquisitionDate(LocalDate.of(2020 + (index % 4), (index % 8) + 1, 15));
    asset.setNotes(index % 3 == 0 ? "Activo verificado en inventario físico." : null);
    asset.replaceAttributeValues(buildAttributeValues(assetType, definition));

    Asset saved = assetRepository.save(asset);
    if (assetTraceabilityRepository.findByAssetIdOrderByOccurredAtDesc(saved.getId()).isEmpty()) {
      assetTraceabilityRepository.save(new AssetTraceability(
          saved,
          TraceabilityEventType.CREATED,
          "Activo registrado por seed de desarrollo.",
          null,
          saved.getCode(),
          "Seed manual dev",
          null
      ));
    }
  }

  private AssetType findType(Category category, String typeName) {
    return category.getTypes().stream()
        .filter(type -> type.getName().equalsIgnoreCase(typeName))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Tipo no encontrado para seed: " + typeName));
  }

  private List<AssetAttributeValue> buildAttributeValues(AssetType assetType, SeedAsset definition) {
    return assetType.getAttributes().stream()
        .map(attribute -> new AssetAttributeValue(attribute, resolveAttributeValue(assetType.getName(), attribute.getName(), definition)))
        .toList();
  }

  private String resolveAttributeValue(String typeName, String attributeName, SeedAsset definition) {
    String normalizedType = typeName.toLowerCase();
    String normalizedAttribute = attributeName.toLowerCase();

    if (normalizedAttribute.contains("marca")) {
      return extractBrand(definition.name());
    }
    if (normalizedAttribute.contains("modelo")) {
      return extractModel(definition.name());
    }
    if (normalizedAttribute.contains("procesador")) {
      return "Intel Core i5";
    }
    if (normalizedAttribute.contains("ram")) {
      return normalizedType.contains("laptop") ? "8" : "16";
    }
    if (normalizedAttribute.contains("almacenamiento")) {
      return normalizedType.contains("tablet") ? "128 GB" : "512 GB SSD";
    }
    if (normalizedAttribute.contains("resolución")) {
      return normalizedType.contains("webcam") ? "1080p" : "Full HD";
    }
    if (normalizedAttribute.contains("lumens")) {
      return "3400";
    }
    if (normalizedAttribute.contains("estándar wi-fi")) {
      return "Wi-Fi 5";
    }
    if (normalizedAttribute.contains("puertos lan")) {
      return "4";
    }
    if (normalizedAttribute.contains("micrófono integrado")) {
      return "Sí";
    }
    if (normalizedAttribute.contains("tecnología")) {
      return normalizedType.contains("impresora") ? "Láser" : "Accesorio";
    }
    if (normalizedAttribute.contains("conectividad")) {
      return "USB / Red";
    }
    if (normalizedAttribute.contains("tamaño")) {
      if (normalizedType.contains("monitor")) return "24 pulgadas";
      if (normalizedType.contains("balón")) return definition.name().contains("#5") ? "N° 5" : "N° 7";
      return "Estándar";
    }
    if (normalizedAttribute.contains("tipo")) {
      if (normalizedType.contains("micrófono")) return "Inalámbrico";
      if (normalizedType.contains("silla")) return "Ergonómica";
      return "Operativo";
    }
    if (normalizedAttribute.contains("frecuencia")) {
      return "2.4 GHz";
    }
    if (normalizedAttribute.contains("longitud")) {
      if (normalizedType.contains("cable hdmi")) return "5 metros";
      return "9 metros";
    }
    if (normalizedAttribute.contains("versión")) {
      return "2.0";
    }
    if (normalizedAttribute.contains("estado físico")) {
      return "Bueno";
    }
    if (normalizedAttribute.contains("alcance")) {
      return "15 metros";
    }
    if (normalizedAttribute.contains("alimentación")) {
      return "2 pilas AAA";
    }
    if (normalizedAttribute.contains("material")) {
      if (normalizedType.contains("escritorio")) return "Melamina";
      if (normalizedType.contains("silla")) return "Metal y tapiz";
      if (normalizedType.contains("archivador")) return "Acero";
      if (normalizedType.contains("estante")) return "Metal reforzado";
      if (normalizedType.contains("balón")) return "Cuero sintético";
      if (normalizedType.contains("colchoneta")) return "Espuma de alta densidad";
      if (normalizedType.contains("red")) return "Nylon";
      return "Plástico reforzado";
    }
    if (normalizedAttribute.contains("dimensiones")) {
      return normalizedType.contains("colchoneta") ? "1 x 2 m" : "120 x 60 cm";
    }
    if (normalizedAttribute.contains("acabado")) {
      return "Color nogal";
    }
    if (normalizedAttribute.contains("número de gavetas")) {
      return "4";
    }
    if (normalizedAttribute.contains("cerradura")) {
      return "Sí";
    }
    if (normalizedAttribute.contains("niveles")) {
      return "5";
    }
    if (normalizedAttribute.contains("capacidad de carga")) {
      return "80 kg";
    }
    if (normalizedAttribute.contains("aumento")) {
      return "1000x";
    }
    if (normalizedAttribute.contains("iluminación")) {
      return "LED";
    }
    if (normalizedAttribute.contains("capacidad")) {
      return normalizedType.contains("balanza") ? "5 kg" : "20 componentes";
    }
    if (normalizedAttribute.contains("precisión")) {
      return "0.01 g";
    }
    if (normalizedAttribute.contains("componentes")) {
      return "Tubos, reactivos básicos y gradillas";
    }
    if (normalizedAttribute.contains("nivel")) {
      return "Secundaria";
    }
    if (normalizedAttribute.contains("implementos de seguridad")) {
      return "Guantes y gafas";
    }
    if (normalizedAttribute.contains("rango de voltaje")) {
      return "0-30 V";
    }
    if (normalizedAttribute.contains("corriente máxima")) {
      return "5 A";
    }
    if (normalizedAttribute.contains("pantalla digital")) {
      return "Sí";
    }
    if (normalizedAttribute.contains("disciplina")) {
      if (definition.name().contains("Fútbol")) return "Fútbol";
      if (definition.name().contains("Básquet")) return "Básquet";
      if (normalizedType.contains("red")) return "Vóley";
      return "Entrenamiento";
    }
    if (normalizedAttribute.contains("altura")) {
      return "30 cm";
    }
    if (normalizedAttribute.contains("color")) {
      return "Naranja";
    }
    if (normalizedAttribute.contains("cantidad por set")) {
      return "20";
    }
    if (normalizedAttribute.contains("espesor")) {
      return "5 cm";
    }
    if (normalizedAttribute.contains("ajustable")) {
      return "Sí";
    }

    return definition.serialNumber();
  }

  private String extractBrand(String assetName) {
    String normalized = assetName
        .replace("Laptop ", "")
        .replace("Desktop ", "")
        .replace("Proyector ", "")
        .replace("Cámara Web ", "")
        .replace("Micrófono Inalámbrico ", "")
        .replace("Puntero Láser ", "")
        .replace("Escritorio ", "")
        .replace("Silla ", "")
        .replace("Archivador ", "")
        .replace("Estante ", "")
        .replace("Microscopio ", "")
        .replace("Balanza ", "")
        .replace("Kit de ", "")
        .replace("Fuente de Poder ", "")
        .trim();
    return normalized.split(" ")[0];
  }

  private String extractModel(String assetName) {
    String[] words = assetName.split(" ");
    if (words.length <= 1) {
      return assetName;
    }
    return String.join(" ", java.util.Arrays.copyOfRange(words, 1, words.length));
  }

  private List<SeedAsset> seedAssets() {
    return List.of(
        new SeedAsset("CMP-2023-045", "Laptop Lenovo ThinkPad T14", "Tecnología", "Laptop", "Laboratorio de Cómputo", "Bueno", "SN: LNV-T14-045"),
        new SeedAsset("CMP-2023-046", "Laptop Lenovo ThinkPad T14", "Tecnología", "Laptop", "Aula 101 - Pabellón A", "Regular", "SN: LNV-T14-046"),
        new SeedAsset("DES-2022-011", "Desktop Dell OptiPlex 7090", "Tecnología", "Desktop", "Laboratorio de Cómputo", "Bueno", "SN: DLL-7090-011"),
        new SeedAsset("DES-2022-012", "Desktop Dell OptiPlex 7090", "Tecnología", "Desktop", "Laboratorio de Cómputo", "Bueno", "SN: DLL-7090-012"),
        new SeedAsset("PRY-2022-012", "Proyector Epson PowerLite E20", "Tecnología", "Proyector", "Auditorio Principal", "Regular", "SN: EPS-E20-012"),
        new SeedAsset("PRY-2022-013", "Proyector Epson PowerLite E20", "Tecnología", "Proyector", "Auditorio Principal", "Bueno", "SN: EPS-E20-013"),
        new SeedAsset("NET-2024-005", "Router TP-Link Archer C80", "Tecnología", "Router", "Aula 101 - Pabellón A", "Bueno", "SN: TPL-C80-005"),
        new SeedAsset("NET-2024-006", "Router TP-Link Archer C80", "Tecnología", "Router", "Aula 101 - Pabellón A", "Regular", "SN: TPL-C80-006"),
        new SeedAsset("VID-2023-017", "Cámara Web Logitech C920", "Tecnología", "Webcam", "Laboratorio de Cómputo", "Malo", "SN: LOG-C920-017"),
        new SeedAsset("IMP-2022-011", "Impresora HP LaserJet Pro M404", "Tecnología", "Impresora", "Sala de Profesores", "Mantenimiento", "SN: HP-M404-011"),
        new SeedAsset("TAB-2024-008", "Tablet Samsung Galaxy Tab A8", "Tecnología", "Tablet", "Aula 101 - Pabellón A", "Bueno", "SN: SAM-TABA8-008"),
        new SeedAsset("TAB-2024-009", "Tablet Samsung Galaxy Tab A8", "Tecnología", "Tablet", "Aula 101 - Pabellón A", "Bueno", "SN: SAM-TABA8-009"),
        new SeedAsset("MON-2023-031", "Monitor LG 24MK430H", "Tecnología", "Monitor", "Laboratorio de Cómputo", "Bueno", "SN: LG-24MK-031"),
        new SeedAsset("MON-2023-032", "Monitor LG 24MK430H", "Tecnología", "Monitor", "Laboratorio de Cómputo", "Bueno", "SN: LG-24MK-032"),
        new SeedAsset("AUD-2022-019", "Micrófono Inalámbrico Shure BLX24", "Tecnología", "Micrófono", "Auditorio Principal", "Bueno", "SN: SHR-BLX24-019"),
        new SeedAsset("ACC-2023-108", "Cable HDMI 5 Metros", "Tecnología", "Cable HDMI", "Auditorio Principal", "Bueno", "SN: HDMI-5M-108"),
        new SeedAsset("ACC-2023-109", "Cable HDMI 5 Metros", "Tecnología", "Cable HDMI", "Auditorio Principal", "Bueno", "SN: HDMI-5M-109"),
        new SeedAsset("ACC-2023-076", "Puntero Láser Kensington", "Tecnología", "Puntero Láser", "Auditorio Principal", "Bueno", "SN: KNS-LSR-076"),
        new SeedAsset("ACC-2023-077", "Puntero Láser Kensington", "Tecnología", "Puntero Láser", "Auditorio Principal", "Bueno", "SN: KNS-LSR-077"),
        new SeedAsset("NET-2021-004", "Switch Cisco 24 Puertos", "Tecnología", "Router", "Laboratorio de Cómputo", "Dado de baja", "SN: CSC-SW24-004"),
        new SeedAsset("MOB-2023-014", "Escritorio Melamina Docente", "Mobiliario", "Escritorio", "Aula 101 - Pabellón A", "Bueno", "SN: MOB-DSK-014"),
        new SeedAsset("MOB-2023-015", "Escritorio Melamina Docente", "Mobiliario", "Escritorio", "Aula 101 - Pabellón A", "Bueno", "SN: MOB-DSK-015"),
        new SeedAsset("MOB-2024-022", "Silla Ergonómica Operativa", "Mobiliario", "Silla", "Sala de Profesores", "Regular", "SN: MOB-CHR-022"),
        new SeedAsset("MOB-2024-023", "Silla Ergonómica Operativa", "Mobiliario", "Silla", "Sala de Profesores", "Bueno", "SN: MOB-CHR-023"),
        new SeedAsset("MOB-2021-005", "Archivador Metálico 4 Gavetas", "Mobiliario", "Archivador", "Sala de Profesores", "Bueno", "SN: MOB-ARC-005"),
        new SeedAsset("MOB-2022-017", "Estante Metálico Reforzado", "Mobiliario", "Estante", "Almacén Deportivo", "Malo", "SN: MOB-EST-017"),
        new SeedAsset("LAB-2024-003", "Microscopio Biológico Binocular", "Laboratorio", "Microscopio", "Laboratorio de Ciencias", "Bueno", "SN: LAB-MIC-003"),
        new SeedAsset("LAB-2024-004", "Microscopio Biológico Binocular", "Laboratorio", "Microscopio", "Laboratorio de Ciencias", "Bueno", "SN: LAB-MIC-004"),
        new SeedAsset("LAB-2023-010", "Balanza Digital Ohaus", "Laboratorio", "Balanza Digital", "Laboratorio de Ciencias", "Regular", "SN: LAB-BAL-010"),
        new SeedAsset("LAB-2023-011", "Balanza Digital Ohaus", "Laboratorio", "Balanza Digital", "Laboratorio de Ciencias", "Bueno", "SN: LAB-BAL-011"),
        new SeedAsset("LAB-2024-021", "Kit de Química Básica", "Laboratorio", "Kit de Química", "Laboratorio de Ciencias", "Bueno", "SN: LAB-KQ-021"),
        new SeedAsset("LAB-2022-009", "Fuente de Poder Regulable", "Laboratorio", "Fuente de Poder", "Laboratorio de Ciencias", "Mantenimiento", "SN: LAB-FTR-009"),
        new SeedAsset("DEP-2024-002", "Balón de Fútbol Molten #5", "Deportes", "Balón", "Almacén Deportivo", "Bueno", "SN: DEP-BAL-002"),
        new SeedAsset("DEP-2024-003", "Balón de Fútbol Molten #5", "Deportes", "Balón", "Almacén Deportivo", "Bueno", "SN: DEP-BAL-003"),
        new SeedAsset("DEP-2023-019", "Conos de Entrenamiento Set x20", "Deportes", "Cono", "Almacén Deportivo", "Bueno", "SN: DEP-CNO-019"),
        new SeedAsset("DEP-2023-020", "Conos de Entrenamiento Set x20", "Deportes", "Cono", "Almacén Deportivo", "Bueno", "SN: DEP-CNO-020"),
        new SeedAsset("DEP-2022-011", "Colchoneta de Gimnasia 1x2 m", "Deportes", "Colchoneta", "Almacén Deportivo", "Regular", "SN: DEP-COL-011"),
        new SeedAsset("DEP-2022-012", "Colchoneta de Gimnasia 1x2 m", "Deportes", "Colchoneta", "Almacén Deportivo", "Bueno", "SN: DEP-COL-012"),
        new SeedAsset("DEP-2021-004", "Red de Vóley Profesional", "Deportes", "Red", "Almacén Deportivo", "Malo", "SN: DEP-RDV-004"),
        new SeedAsset("DEP-2024-014", "Balón de Básquet Spalding", "Deportes", "Balón", "Almacén Deportivo", "Dado de baja", "SN: DEP-BSQ-014")
    );
  }

  private record SeedCategory(String name, String icon, List<SeedType> types) {}

  private record SeedType(String name, String icon, List<SeedAttribute> attributes) {}

  private record SeedAttribute(String name, String description, boolean required) {}

  private record SeedLocation(String name, String description) {}

  private record SeedSupplier(String name, String ruc, String email, String phone, String address) {}

  private record SeedTeacher(String dni, String fullName, String specialty, String email, String phone) {}

  private record SeedAsset(
      String code,
      String name,
      String categoryName,
      String typeName,
      String locationName,
      String conditionLabel,
      String serialNumber
  ) {}
}
