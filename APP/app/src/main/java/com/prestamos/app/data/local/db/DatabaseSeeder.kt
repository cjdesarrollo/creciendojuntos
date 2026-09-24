package com.prestamos.app.data.local.db

import com.prestamos.app.data.repository.LoanRepository
import kotlinx.coroutines.flow.first
import java.math.BigDecimal

class DatabaseSeeder(
    private val repository: LoanRepository,
    private val configRepository: com.prestamos.app.data.repository.ConfigRepository? = null
) {

    suspend fun seedSampleDataIfEmpty() {
        seedCapitalOriginsIfEmpty()
        seedCompaniesAndUsersIfEmpty()

        val existingClients = repository.getAllClients().first()
        if (existingClients.isNotEmpty()) {
            return // La base de datos ya tiene clientes registrados
        }

        // Cargar los 45 clientes reales desde la plantilla de importación
        val clients = listOf(
            com.prestamos.app.domain.model.Client(
                fullName = "Belen del Rosario  Mercado Rodriguez",
                dniOrId = "0840610770000V",
                phone = "58672043",
                address = "CHINANDEGA, CHICHIGALPA, RPTO ERICK RAMIREZ, EMPALME CHICHIGALPA 200 METROS AL SUR.",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "LUIS ALBRTO PASTRAN",
                dniOrId = "2811510900023V",
                phone = "81529935",
                address = "LEON, LEON, RPTO. 1RO DE MAYO, CENTRO DE SALUD 5C AL  NORTE, 10 VRS AL ESTE.",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "LUISA AMANDA MARTINEZ",
                dniOrId = "0811203011004Q",
                phone = "88358049",
                address = "LEON, LEON, RPTO. ZARAGOZA, DE LA IGLESIA 1C AL OETSE, 1/2C AL SUR.",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "LESBERT ALTAMIRANO",
                dniOrId = "2812211910015D",
                phone = "88358049",
                address = "LEON, LEON, RPTO. ZARAGOZA, DE LA IGLESIA 1C AL OESTE, 1/2C AL SUR.",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "KAREN DE SOCORRO TERCERO LOPEZ",
                dniOrId = "2810811830008J",
                phone = "86937349",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "Lester Salomon Reed",
                dniOrId = "0013012780041B",
                phone = "83701568",
                address = "Managua",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "MARIA ANGELES AVENDAÑO GOMEZ",
                dniOrId = "2811602011006X",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "Yader Jose Caballero Quezada",
                dniOrId = "2811002880005Y",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "Ada Francis Martinez Mejia",
                dniOrId = "2810910760013X",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "YUVELKA DEL SOCORRO AGUILAR ESPINALES",
                dniOrId = "2811911870006J",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "Lesther  Johany Altamirano Jaen",
                dniOrId = "2811606021008V",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "Jose Daniel Caceres Perez",
                dniOrId = "2812101710000D",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "Luis Alcides Marin Hidalgo",
                dniOrId = "2882308590001R",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "XOCHILT MASSIEL HERNANDEZ RAMIREZ",
                dniOrId = "2812207940002W",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "Gabriela Alejandra Mercado",
                dniOrId = "2810411960011H",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "Karen De Los Angeles Berrios Morales",
                dniOrId = "2812202640000A",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "Violeta Esperanza   Roque Trujillo",
                dniOrId = "2813005720006A",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "YADER ANTONIO BERRIOS MARTINEZ",
                dniOrId = "2810606730007A",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "Joaquin Enmanuel Murillo Maldonado",
                dniOrId = "2840403950001C",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "Ligia Mercedes Morales Lezama",
                dniOrId = "2810201730015S",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "DANIESKA NAYMA RIVERA CORTEZ",
                dniOrId = "2810106041007B",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "Anabell Mendoza Balladares",
                dniOrId = "2813008800003X",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "ANIELKA MARIA CORTEZ GUTIERREZ",
                dniOrId = "2811403830008U",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "Yeysi de los Angeles Lindo Ruiz",
                dniOrId = "2810802840004Q",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "Henry Ernesto Crespin",
                dniOrId = "2810603900006M",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "Susana Itamar Salinas Salazar",
                dniOrId = "2811211971005A",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "JORGE HUMBERTO MENDOZA BALLADAFRES",
                dniOrId = "2810408770003H",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "MARCIO BENITO RUGAMA MENDEZ",
                dniOrId = "2810110021007N",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "Maximo Guillermo Alonso Delgadillo",
                dniOrId = "2810610820003X",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "EDDY ALFONSO LOPEZ DIAZ",
                dniOrId = "2810209930003X",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "EDDA CUEVAS",
                dniOrId = "2810408770003H",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "JUAN PABLO LOPEZ RUBI",
                dniOrId = "2810803700013H",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "JOSHUA JAFET HURTADO CACERES",
                dniOrId = "2882308590001R",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "AIDA ENCARNACION PALMA ORTIZ",
                dniOrId = "2810106041007B",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "Eduardo David Guardado Jaen",
                dniOrId = "2811212860007G",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "ENMI LISSETTE PICADO REYES",
                dniOrId = "2810802840004Q",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "MARIA ANGELES AVENDAÑO GOMEZ",
                dniOrId = "2811602011006X",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "KARLA LUCIA LOPEZ GARCIA",
                dniOrId = "2910309770000H",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "kevin Antonio Medal Zapata",
                dniOrId = "2811310021012U",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "Alison Oriana Mendoza Perez",
                dniOrId = "2812204980001E",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "Freddy  Mauricio Guardado Jaenz",
                dniOrId = "2811511830004U",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "Lesther  Del Carmen Altamirano Ruiz",
                dniOrId = "290 160777 0001G",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "Maura Muñoz Gonzalez",
                dniOrId = "2811302570000T",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "Mauro Gasbarra",
                dniOrId = "7772603560000K",
                phone = "",
                address = "Managua",
                estimatedInterestPercent = BigDecimal("10.00")
            ),
            com.prestamos.app.domain.model.Client(
                fullName = "Ramiro Jesus Sánchez Lopez",
                dniOrId = "2812312981000U",
                phone = "",
                address = "leon",
                estimatedInterestPercent = BigDecimal("10.00")
            )
        )

        clients.forEach { client ->
            repository.saveClient(client)
        }
    }

    suspend fun seedCapitalOriginsIfEmpty() {
        val existingOrigins = repository.getAllCapitalOrigins().first()
        if (existingOrigins.isNotEmpty()) {
            return
        }

        // 1. Capital Propio Creciendo Juntos
        repository.saveCapitalOrigin(
            com.prestamos.app.domain.model.CapitalOrigin(
                id = 0,
                name = "Capital Propio Creciendo Juntos",
                description = "Fondos directos de la empresa Creciendo Juntos para microcréditos comunitarios",
                currentBalance = BigDecimal("250000.00"),
                companyId = 1L,
                isActive = true,
                totalInvested = BigDecimal("50000.00"),
                totalReturned = BigDecimal("15000.00")
            )
        )

        // 2. Inversionista Capital Privado A
        repository.saveCapitalOrigin(
            com.prestamos.app.domain.model.CapitalOrigin(
                id = 0,
                name = "Inversionista Capital Privado A",
                description = "Fondos aportados por inversionistas privados con tasa preferencial compartida",
                currentBalance = BigDecimal("150000.00"),
                companyId = 0L, // Compartido
                isActive = true,
                totalInvested = BigDecimal("30000.00"),
                totalReturned = BigDecimal("8000.00")
            )
        )

        // 3. Fondo Rotatorio Facilito
        repository.saveCapitalOrigin(
            com.prestamos.app.domain.model.CapitalOrigin(
                id = 0,
                name = "Fondo Rotatorio Facilito",
                description = "Fondo de liquidez rápida para microcréditos inmediatos Facilito",
                currentBalance = BigDecimal("100000.00"),
                companyId = 2L,
                isActive = true,
                totalInvested = BigDecimal("20000.00"),
                totalReturned = BigDecimal("5000.00")
            )
        )

        // 4. Socio Estratégico Norte
        repository.saveCapitalOrigin(
            com.prestamos.app.domain.model.CapitalOrigin(
                id = 0,
                name = "Socio Estratégico Norte",
                description = "Línea de crédito para financiamiento comercial y productivo",
                currentBalance = BigDecimal("80000.00"),
                companyId = 0L, // Compartido
                isActive = true,
                totalInvested = BigDecimal("15000.00"),
                totalReturned = BigDecimal("3000.00")
            )
        )
    }

    suspend fun seedCompaniesAndUsersIfEmpty() {
        val configRepo = configRepository ?: return

        // 1. Seed Empresas
        val existingCompanies = configRepo.getAllCompanies().first()
        if (existingCompanies.isEmpty()) {
            configRepo.saveCompany(
                com.prestamos.app.domain.model.CompanyProfile(
                    id = 1L,
                    name = "Creciendo Juntos",
                    phone = "8232 2262",
                    address = "Managua, Nicaragua",
                    ruc = "J0310000123456",
                    email = "contacto@creciendojuntos.ni",
                    defaultInterestRate = BigDecimal("10.00")
                )
            )

            configRepo.saveCompany(
                com.prestamos.app.domain.model.CompanyProfile(
                    id = 2L,
                    name = "Facilito",
                    phone = "8797 3321",
                    address = "León, Nicaragua",
                    ruc = "J0310000654321",
                    email = "atencion@facilito.ni",
                    defaultInterestRate = BigDecimal("12.00")
                )
            )
        }

        // 2. Seed Usuarios
        val existingUsers = configRepo.getAllActiveUsers().first()
        if (existingUsers.isEmpty()) {
            configRepo.saveUser(
                com.prestamos.app.domain.model.AppUser(
                    id = 0,
                    fullName = "Walter Administrador",
                    username = "admin",
                    phone = "8232 2262",
                    role = "Administrador",
                    pin = "1234",
                    photoUri = null,
                    biometricEnabled = true
                )
            )

            configRepo.saveUser(
                com.prestamos.app.domain.model.AppUser(
                    id = 0,
                    fullName = "Agente Cobrador",
                    username = "cobrador1",
                    phone = "8797 3321",
                    role = "Cobrador",
                    pin = "0000",
                    photoUri = null,
                    biometricEnabled = false
                )
            )
        }
    }
}
