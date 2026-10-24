# Railway GPS

**Projektbeskrivning och teknisk specifikation – sammanställning av våra beslut och idéer**  
**Målplattform:** Minecraft 26.3, Fabric  
**Status:** Planeringsunderlag, inte färdig implementationsspecifikation

## 1. Vision

**Railway GPS** är ett navigationsmod för stora järnvägsnät på en Minecraft-server. En spelare ska kunna välja en station eller annan registrerad destination och få hjälp av en HUD som visar vilken väg spelaren ska ta, exempelvis **"Välj vänster vid nästa rödljus"**.

Modden ska **inte förändra vanilla-gameplay**. Den ska inte styra minecarten, trycka på knappar, påverka räls, växlar eller redstone och inte automatiskt välja väg åt spelaren. Den ska enbart läsa relevant information och ge navigationshjälp.

En administratör bygger och underhåller serverns järnvägskarta. Kartan överförs automatiskt till spelare med Railway GPS installerat. **JourneyMap ska kunna integreras men inte vara ett krav.**

### Grundprinciper

- Navigationsnätverket är **manuellt administrerat**: administratörer väljer stationer, rödljus och vilka anslutningar som ska finnas.
- Själva **spårgeometrin spelas in** när administratören åker med en minecart från en registrerad punkt till en annan.
- Rödljus kan vara byggda på olika sätt. GPS:en kräver **ingen standardiserad redstone-konstruktion** och behöver inte känna till knapparnas exakta positioner.
- Varje station, rödljus och inspelad väg får **en egen JSON-fil** på servern.
- Varje inspelad väg har ett **unikt tekniskt ID** och ett separat **visningsnamn**.
- Servermodden håller nätverket aktuellt; klientmodden räknar rutter, följer spelarens position och visar HUD.

## 2. Befintlig järnvägsmiljö

På servern finns långa järnvägar med korsningar som kallas **rödljus**. En minecart stannar vid rödljuset, och spelaren väljer manuellt väg. Den beskrivna varianten har flera knappar och lampor för val av färdväg samt en knapp för att åka tillbaka.

**Rödljusen kan se olika ut.** Railway GPS ska därför behandla dem som geografiska *navigationspunkter* med anslutande järnvägssträckor, inte som en särskild maskintyp.

Begrepp:

| Begrepp | Betydelse |
|---|---|
| **Station** | Namngiven destination eller startpunkt i järnvägsnätet. |
| **Rödljus / korsning** | Navigationspunkt där spelaren normalt kan välja fortsatt väg. |
| **Väg / sträcka / route** | Namngiven, inspelad järnvägsförbindelse mellan två registrerade punkter. |
| **Spårpunkter** | Koordinater som beskriver hur en inspelad järnväg faktiskt går, inklusive svängar och höjdskillnader. |
| **Järnvägsgraf** | Nätverk av stationer/rödljus (noder) och inspelade vägar (kanter). |

## 3. Övergripande arkitektur

### Serverdel (Fabric)

Servern ansvarar för att:

1. Läsa och spara individuella JSON-filer för stationer, rödljus och vägar.
2. Registrera administratörskommandon för att skapa och hantera nätverket.
3. Spela in en administratörs minecart-färd mellan två valda punkter.
4. Validera nätverket och bygga en sammanställd graf i minnet.
5. Synkronisera det färdiga nätverket till klienter som har Railway GPS.
6. Skicka uppdateringar när en väg läggs till, tas bort eller när nätverket laddas om.

Serverdelen ska vara lättviktig och inte påverka hur järnvägen fungerar.

### Klientdel (Fabric)

Klienten ansvarar för att:

1. Ta emot serverns aktuella järnvägskarta.
2. Låta spelaren välja destination.
3. Beräkna en körbar rutt via järnvägsgrafen.
4. Uppskatta var spelaren befinner sig längs de inspelade sträckorna.
5. Visa instruktioner i en HUD och beräkna om rutten vid avvikelse.
6. Valfritt visa stationer, rödljus och järnvägssträckor via JourneyMap.

**Installationsidé:** samma Railway GPS-mod kan ha både server- och klientfunktioner, med korrekt separation så att klientens renderingskod inte laddas på en dedikerad server. Fabric API används där det behövs. JourneyMap är en valfri klientintegration.

Det är tänkt att vanliga spelare utan Railway GPS fortfarande ska kunna ansluta till servern och spela vanilla som vanligt. Servern skickar endast modspecifika nätverkspaket till kompatibla klienter.

> **Att verifiera vid implementation:** exakta kompatibla utgåvor och API-versioner för Minecraft 26.3, Fabric, Fabric API och JourneyMap.

## 4. Lagring på servern

Varje objekt sparas i **en separat JSON-fil**. Filerna hör till den aktuella Minecraft-världen, exempelvis:

```text
<världsmapp>/
└── railwaygps/
    ├── stations/
    │   ├── centralstationen.json
    │   ├── spawn_station.json
    │   └── norra_station.json
    ├── junctions/
    │   ├── rodljus1.json
    │   ├── rodljus2.json
    │   └── rodljus3.json
    └── routes/
        ├── norra_bron.json
        ├── spawn_till_rodljus1.json
        └── sodra_tunneln.json
```

I många serverinstallationer heter världsmappen `world`, men sökvägen ska följa serverns verkliga världsmapp.

### Station (`stations/centralstationen.json`)

```json
{
  "id": "centralstationen",
  "name": "Centralstationen",
  "dimension": "minecraft:overworld",
  "position": [1250, 64, -350]
}
```

### Rödljus (`junctions/rodljus1.json`)

```json
{
  "id": "rodljus1",
  "name": "Rödljus 1",
  "dimension": "minecraft:overworld",
  "position": [1450, 70, -500],
  "radius": 25
}
```

`radius` är ett tänkt inställbart identifieringsområde, inte en garanti för att ett rödljus identifieras enbart på avstånd. Den faktiska positionsmatchningen måste också ta hänsyn till inspelade rälssträckor och tidigare färdriktning.

### Väg (`routes/norra_bron.json`)

```json
{
  "id": "norra_bron",
  "name": "Norra bron",
  "from": "rodljus1",
  "to": "centralstationen",
  "dimension": "minecraft:overworld",
  "bidirectional": false,
  "distance": 850.0,
  "points": [
    [1450, 70, -500],
    [1480, 70, -500],
    [1490, 71, -510],
    [1500, 72, -530],
    [1250, 64, -350]
  ]
}
```

**Obs:** Alla koordinater och sträcklängder i exemplen är påhittade. Under riktig inspelning skapas spårpunkter och beräknad längd automatiskt. Det exakta JSON-schemat behöver fastställas vid implementationen.

### Lagringsregler

- **Tekniskt ID** är unikt och kan användas i kommandon och som filnamn: `norra_bron`.
- **Visningsnamn** får vara läsbart och kan ändras utan att anslutningens ID ändras: `Norra bron`.
- En väg refererar till startpunkt och slutpunkt via deras **ID:n**.
- Stationer och rödljus behöver **inte** innehålla listor över anslutna vägar. Den informationen byggs från filerna i `routes/`.
- Det kan finnas **flera olika vägar mellan samma två noder**; de skiljs åt med sina unika väg-ID:n.
- Samma ID ska inte kunna användas för flera olika noder.
- Raderade vägfiler ska inte radera stationer eller rödljus.
- En nod som används av någon väg får inte raderas utan att beroendena hanteras först.
- Filnamn/ID:n bör valideras för att hindra felaktiga sökvägar, dubbletter och filnamnskrockar.
- Uppdateringar ska skrivas **atomärt** (tillfällig fil + säker ersättning) så att avbruten skrivning inte förstör sparade data.

## 5. Administratörens arbetsflöde

### 5.1 Registrera punkter

Administratören registrerar stationer och rödljus vid deras ungefärliga koordinater i världen. Punkterna representerar geografiska navigationsplatser och kräver inga specifika knappar eller block.

Föreslagna kommandon:

```mcfunction
/rgps node add rodljus1
/rgps station add centralstationen
```

Dessa kommandon skapar varsin JSON-fil i korrekt mapp. Detaljer som visningsnamn och sökradie kan senare ändras i JSON eller med fler admin-kommandon.

### 5.2 Spela in en järnvägssträcka

Administratören sitter i en minecart vid startpunkten och anger **startnod, målnod och ett unikt väg-ID**:

```mcfunction
/rgps record start rodljus1 rodljus2 norra_bron
```

Modden spelar därefter in den faktiska färden medan administratören **själv kör** från `rodljus1` till `rodljus2` och gör de vägval som behövs vid korsningar.

Vid framkomst avslutar administratören uttryckligen inspelningen:

```mcfunction
/rgps record finish
```

Först då valideras och sparas resultatet som exempelvis:

```text
<världsmapp>/railwaygps/routes/norra_bron.json
```

När målets område upptäcks kan modden föreslå att inspelningen avslutas, men ska **inte** automatiskt spara bara för att minecarten kommer nära målkoordinaterna. Spår kan ligga parallellt eller på olika höjd.

### 5.3 Strikt avbrytande om administratören lämnar minecarten

**Beslutat beteende:** om administratören lämnar minecarten under inspelningen ska inspelningen **avbrytas helt direkt**, inte pausas.

- Alla tillfälliga inspelningsdata kastas.
- Ingen ny vägfil skapas eller skrivs över.
- Administratören får ett tydligt meddelande om att inspelningen avbröts.
- Inspelningen kopplas till **den specifika minecart** som administratören satt i när den startade.

Samma avbrytanderegel föreslås för frånkoppling, död, dimensionsbyte, förstörd minecart eller byte till en annan minecart.

Manuellt avbrytkommando:

```mcfunction
/rgps record cancel
```

### 5.4 Föreslagna admin-kommandon

| Kommando | Funktion |
|---|---|
| `/rgps node add <id>` | Registrera ett rödljus vid administratörens position. |
| `/rgps station add <id>` | Registrera en station vid administratörens position. |
| `/rgps record start <från> <till> <väg_id>` | Starta inspelning av namngiven järnvägssträcka. |
| `/rgps record finish` | Avsluta, validera och spara inspelningen. |
| `/rgps record cancel` | Avbryt utan att spara något. |
| `/rgps record status` | Visa pågående inspelning, längd och antal punkter. |
| `/rgps route list` | Lista befintliga vägar. |
| `/rgps route info <väg_id>` | Visa detaljer om en väg. |
| `/rgps route remove <väg_id>` | Ta bort endast den namngivna vägen. |
| `/rgps route rename <gammalt_id> <nytt_id>` | Ändra en vägs tekniska ID/filnamn. |
| `/rgps network list` | Visa registrerade förbindelser. |
| `/rgps reload` | Läs om, validera och synkronisera JSON-filerna. |

**Obs:** Kommandona ovan är föreslagen kommandodesign, inte redan implementerade kommandon. Administrationskommandon ska vara behörighetskontrollerade.

## 6. Hur inspelning av räls ska fungera tekniskt

Inspelningen görs på **serversidan** medan administratören färdas i en minecart.

### Primär insamling

- Läs minecartens position och rörelse regelbundet, lämpligen i samband med server-ticks (vanligen 20 per sekund vid normal serverprestanda).
- Undersök rälsblocket som vagnen faktiskt följer när det är möjligt.
- Registrera ordningen på de passerade spårpositionerna och rälsens form: raka partier, kurvor och lutningar.
- Spara höjd (`Y`) samt `X` och `Z` så att broar, tunnlar och nivåskillnader skiljs åt.
- Duplicera inte punkter i onödan när vagnen står stilla.

### Efterbearbetning

- Beräkna sträckans **faktiska färdlängd** utifrån den inspelade vägen, inte bara fågelvägen.
- Komprimera långa raka avsnitt till färre punkter men **behåll kurvor, lutningar och viktig geometri**.
- Var försiktig vid parallella spår, korsningar och slingor så att förenklingen inte ger en missvisande rutt.
- Behåll tillräckliga spårpunkter för positionsmatchning och kartöverlägg.

### Inspelningsregler

- En inspelad färd representerar minst en **riktad** anslutning, `från → till`.
- **Dubbelriktning antas inte automatiskt.** Returresa kan spelas in separat eller uttryckligen markeras som tillåten om den har verifierats.
- Om färden passerar ett redan registrerat mellanliggande rödljus bör modden varna och på sikt kunna hjälpa administratören dela upp vägen i flera sträckor.
- Om vägen redan har ett befintligt ID ska administratören inte råka skriva över filen utan ett särskilt, bekräftat ersättningsflöde.

## 7. Nätverksmodell och ruttberäkning

### Grafen

- **Noder:** stationer och rödljus.
- **Kanter:** namngivna, inspelade vägar mellan noder.
- **Kantvikt:** inspelad körsträcka (senare eventuellt uppskattad restid).
- **Riktning:** varje kant har tillåten färdriktning; dubbelriktning måste anges eller verifieras.
- **Parallella kanter:** flera olika vägar får förbinda samma noder.

Exempel:

```text
Spawnstation ──(spawn_norr)──> Rödljus 1
                                   │
                                   ├──(norra_bron)──> Rödljus 2 ──> Centralstationen
                                   │
                                   └──(sodra_tunneln)──> Rödljus 3 ──> Centralstationen
```

Klienten kan använda **Dijkstra** eller **A\*** för att välja kortaste tillåtna väg genom nätet.

### Viktig navigationsbegränsning

Att en korsning har anslutningar till A, B och C betyder **inte nödvändigtvis** att alla svängar går att göra från varje infart. Därför bör ruttsystemet på sikt kunna hantera **tillåtna/otillåtna övergångar** vid en korsning, baserat på vilken väg spelaren kommer från och vilken väg spelaren ska fortsätta på.

Som en första prototyp kan det finnas en förenklad standardregel för korsningar, men vi ska inte presentera omöjliga övergångar som säkert körbara när de inte är verifierade.

## 8. Positionsbestämning och omplanering

GPS:en ska inte behöva veta exakt när spelaren stannar i ett rödljus eller trycker på en knapp.

I stället arbetar klienten med **map matching** mot de inspelade rälssträckorna:

1. Läser spelarens/minecartens aktuella position och färdriktning.
2. Jämför positionen mot de inspelade spårgeometrierna i relevant dimension.
3. Tar hänsyn till **föregående bekräftade sträcka**, färdriktning och nätverkets möjliga fortsättningar.
4. Följer hur långt längs den aktuella sträckan spelaren kommit.
5. Bekräftar nästa rödljus/station när både position och färdhistorik stödjer det.

Ett möjligt internt flöde är:

```text
På känd sträcka → Närmar sig rödljus → Rödljus bekräftat → Ny sträcka identifierad
```

Sökradier kring noder och en viss **hysteres** förhindrar att samma rödljus identifieras upprepade gånger när vagnen står stilla.

### När spelaren tar fel väg

- Om minecarten följer en annan inspelad förbindelse än den planerade ska GPS:en **räkna om** rutten.
- När flera spår ligger tätt eller parallellt måste klienten få visa **osäker position** i stället för att gissa.
- I svåra fall kan säker omplanering behöva vänta tills nästa registrerade punkt eller sträcka kan identifieras.

Den inspelade geometrin gör detta betydligt säkrare än att bara välja närmaste rödljus utifrån koordinater.

## 9. Navigationsinstruktioner och HUD

Spelaren väljer destination och får exempelvis:

```text
RAILWAY GPS
Destination: Centralstationen

NÄSTA KORSNING: Rödljus 2
Välj höger
215 block kvar

Återstående järnväg: 2 450 block
Korsningar kvar: 3
```

Möjliga HUD-funktioner:

- Nästa rödljus/station.
- **Vänster, höger, rakt fram eller tillbaka** där instruktionen går att fastställa.
- Avstånd längs inspelad järnväg till nästa korsning och destination.
- Återstående antal korsningar.
- Indikator för **på rätt spår**, **omräkning** eller **osäker position**.
- Visa främst navigations-HUD när spelaren sitter i en minecart.

### Hur vänster/höger fastställs

Den inspelade rälsens **faktiska ingångs- och utgångsriktning** vid ett rödljus ger ett bättre geometriskt underlag än en rak linje mellan rödljusens mittpunkter.

Det går dock inte att garantera att geometriskt "vänster" alltid motsvarar rätt **knappval** i ett specialbyggt rödljus. Därför föreslås **valfria manuella instruktioner** för specifika övergångar, till exempel: *"Kommer från norra_bron och ska vidare via sodra_tunneln → välj den högra knappen"*.

I första versionen bör GPS:en hellre ge en neutral instruktion, till exempel **"Fortsätt mot Rödljus 3"**, än ett osäkert höger-/vänsterbesked.

## 10. JourneyMap-integration

JourneyMap ska vara **valfri** och får inte vara ett krav för att ruttberäkningen eller HUD:en ska fungera.

Önskade funktioner:

- Visa registrerade **stationer** och **rödljus** på kartan.
- Visa inspelade **järnvägssträckor som linjer längs deras faktiska geometri**.
- Markera den aktuellt beräknade rutten.
- Eventuellt låta spelaren välja en station/destination via JourneyMap eller dess waypoints.

Railway GPS hanterar fortfarande sin **egen graf och datalagring**. JourneyMap används för kartvisualisering och eventuellt val av mål. Exakta API-möjligheter och versionsstöd ska kontrolleras innan denna integration byggs.

## 11. Synkronisering och omladdning

Föreslaget flöde vid anslutning:

1. Servern har laddat alla stations-, junction- och route-filer.
2. Servern validerar att referenser, dimensioner och format är korrekta.
3. Den färdiga kartan byggs i minnet och förses med en nätverksversion.
4. Railway GPS-klienter får kartan via Fabric Networking.
5. Klienten bygger en lokal navigationsgraf och kan börja navigera.

Vid ändringar eller `/rgps reload`:

- Läs in filerna till en **ny tillfällig nätverksmodell**.
- Kontrollera hela modellen innan den aktiveras.
- Om JSON eller referenser är trasiga: rapportera vilket objekt som felar och **behåll den senast fungerande kartan**.
- Om allt är giltigt: ersätt serverns aktiva karta och synkronisera klienterna.
- En klient som navigerar längs en borttagen väg ska **räkna om rutten**, eller rapportera att ingen väg längre finns.

För stora nätverk bör synkroniseringen använda ett versions-/hashvärde och vid behov lämplig paketuppdelning. Själva filstrukturen på servern behöver inte speglas en-till-en hos spelarna; klienten behöver främst den färdiga modellen.

## 12. Säkerhet, stabilitet och gränser

- Endast behöriga administratörer får skapa, spela in, byta namn på och radera nätverksobjekt.
- Ingen automatisering av knappar, redstone, räls eller vagnens styrning.
- Ingen ny väg sparas förrän administratören uttryckligen avslutat en giltig inspelning.
- Att lämna den aktiva minecarten **avbryter omedelbart inspelningen utan att spara**.
- Filskrivningar görs säkert och sparade filer valideras före aktivering.
- ID:n och filnamn kontrolleras så att kommandon inte kan skriva utanför järnvägsmappen.
- Parallella spår, nivåskillnader och överlappande sträckor måste hanteras försiktigt vid positionsbestämning.
- En inspelad resa är **inte** i sig bevis för att motsatt riktning fungerar.
- Att två vägar möts vid ett rödljus är **inte** i sig bevis för att alla anslutningssvängar går att göra.
- Ingen förutsättning att JourneyMap finns installerat.

## 13. Prioriterad utvecklingsplan

### Fas 1 – Grundläggande servernätverk

- Fabric-projekt och separat server-/klientkod.
- Filstruktur: `stations/`, `junctions/`, `routes/`.
- JSON-inläsning, validering och centralt nätverksobjekt.
- Admin-kommandon för att skapa stationer och rödljus.

### Fas 2 – Inspelning

- `/rgps record start <från> <till> <väg_id>`.
- Registrering av minecart och rälsgeometri.
- Fullt avbrott när administratören lämnar minecarten.
- `/rgps record finish`, `cancel`, `status`.
- Namngivna filer, listning, borttagning och omdöpning av vägar.

### Fas 3 – Enkel navigation

- Synkronisering från server till klient.
- Destinationer, graf och kortaste väg.
- Grundläggande map matching längs inspelade sträckor.
- Enkel HUD med nästa navigationspunkt och avstånd.

### Fas 4 – Pålitliga vägval

- Förbättrade höger-/vänster-/rakt-fram-instruktioner.
- Valfria manuella instruktioner för specialkorsningar.
- Stöd för begränsade övergångar vid rödljus.
- Omplanering vid fel vägval och tydlig osäkerhetsindikering.

### Fas 5 – JourneyMap och förbättrad administration

- Visa stationer, rödljus, spår och beräknad rutt.
- Eventuell destinationsväljare via JourneyMap.
- Mer avancerade admin-kommandon och förbättrad kartredigering.

## 14. Saker som återstår att besluta/testa

Detta är fortfarande **öppna implementationfrågor**, inte färdiga beslut:

1. Exakt hur en station/rödljus positioneras, exempelvis på spelarens koordinat, på rälsblocket eller med särskilt placerad markör.
2. Hur en inspelning säkert kontrollerar att start och mål motsvarar angivna noder.
3. Hur spårgeometrin representeras och förenklas utan att parallella spår sammanblandas.
4. Hur korsningar med otillåtna övergångar konfigureras utan att systemet blir komplicerat.
5. Hur instruktioner vid olika specialbyggda rödljus ska kunna åsidosättas manuellt.
6. Exakt syntax för att redigera visningsnamn, sökradier, dubbelriktning och övrig metadata.
7. Exakt API-stöd och kompatibilitet för den aktuella Minecraft-/Fabric-/JourneyMap-versionen.
8. Om navigering ska kunna starta på vilken punkt som helst längs en inspelad sträcka eller om första versionen förutsätter start nära en registrerad nod.

## 15. Sammanfattning av fastslagna önskemål

Railway GPS ska vara ett **icke-ingripande GPS-mod** för ett befintligt vanilla-järnvägsnät. Serveradministratörer registrerar stationer och rödljus som geografiska punkter och spelar in **namngivna järnvägssträckor** genom att faktiskt åka dem i en minecart. Inspelningen fångar kurvor, lutningar och den verkliga vägen. Om administratören lämnar minecarten **avbryts allt omedelbart utan sparning**.

Varje station, rödljus och väg ska ha **en egen fil** i mapparna `stations/`, `junctions/` respektive `routes/` i världens `railwaygps/`-katalog. En lättviktig Fabric-serverdel distribuerar nätverket till klienterna. Klienten använder spårgeometrin för positionsbestämning, ruttberäkning och en HUD med körinstruktioner. JourneyMap kan integreras för kartvisning men ska vara valfritt. Inget i modden ska ändra hur vanilla-järnvägen fungerar.
