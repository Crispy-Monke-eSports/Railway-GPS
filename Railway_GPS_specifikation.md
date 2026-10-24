# Railway GPS

**Produkt- och teknisk målbild**

**Målplattform:** Minecraft Java Edition 26.3, Fabric

**Status:** Sammanhängande planeringsspecifikation för nästa arkitektur

Den här specifikationen beskriver den önskade lösningen utan krav på bakåtkompatibilitet med en tidigare datamodell. Exakta kommandon och lagringsscheman får förfinas under implementation, men domänmodellen och användarbeteendet nedan är vägledande.

**Fastställda produktbeslut (2026-10-25):**
- **Tre enkla användarbegrepp:** stationer (enda destinationerna), rödljus (vägval i en black box) och namn på valfria rälssträckor (t.ex. Riksettan och Centralbron). **Ingen separat terminaltyp, inget plattformssystem och inga extra obligatoriska stationsobjekt.**
- **Station:** ett namngivet stoppställe direkt på rälsen, där spåret tar slut eller där vagnen kan stanna och sedan fortsätta/vända. Ankomst vid rätt stoppunkt/stoppzon på rätt spår bekräftas **även när vagnen rullar**. Visa **Framme** tills spelaren lämnar minecarten; avsluta då navigeringen automatiskt.
- **Osäker position:** visa destination och **Osäker position**, men **inga sväng- eller knappinstruktioner** förrän rätt spår identifierats.
- **Survey:** starta var som helst på rälsen; hela inspelningen får publiceras **direkt och atomiskt** vid `finish` bara när anslutningen till det registrerade nätet och övrig validering är säkra. Ingen obligatorisk manuell publiceringsbekräftelse.
- **Parallella spårpar:** geometribaserade förslag, alltid **uttryckligt godkännande från administratör**; annars förblir spåren självständiga.
- **HUD-namn:** visa en **huvudled plus högst ett lokalt namn**, t.ex. `Riksettan · Centralbron`, när de finns.
- **Administratörsinstruktioner:** visas **ordagrant**, oberoende av spelarens språk; endast inbyggda standardtexter översätts.
- **Behörigheter:** separata serverrättigheter för inspektion, namngivning, inspelning samt administration/publicering.
- **Rödljus:** endast in-/utgående spår, verifierade vägval och instruktioner. Ingen intern räls, redstone-modell eller genomfartskostnad.

## 1. Vision

Railway GPS är ett icke-ingripande navigationsmod för stora befintliga järnvägsnät. Spelaren väljer en **station** och får hjälp att följa rätt spår genom **rödljus**. Administratörer kan **namnge särskilda rälssträckor**. Det krävs inga andra spelarsynliga platstyper, som terminaler eller plattformar. Rödljus är aldrig destinationer.

Moddet ska kunna ge instruktioner som:

- **Fortsätt rakt fram**
- **Fortsätt åt vänster**
- **Fortsätt åt höger**
- **Vänd tillbaka**
- **Fortsätt på Riksettan**
- **Fortsätt åt vänster in på Hamnleden**
- **Välj knappen för Martyrön**

Moddet ska inte styra minecarten, trycka på knappar, ändra redstone, lägga om växlar eller på annat sätt påverka vanilla-järnvägen. Det observerar färden och visar navigationshjälp.

Servern äger och validerar järnvägsnätet. Klienten tar emot en säker, versionsmärkt vy av nätet och ansvarar för presentation, positionsmatchning och navigeringsupplevelse. JourneyMap är en valfri visualisering, inte ett krav.

## 2. Grundprinciper

### 2.1 Fysik, vägval och namn är separata

Systemet ska skilja mellan:

1. **Fysisk räls:** var de faktiska spåren går.
2. **Tillåtna övergångar:** vilka vägval som går att göra i en korsning.
3. **Semantiska namn:** exempelvis Riksettan, Centralbron eller Södra tunneln.
4. **Beräknad resa:** den kombination av riktade spår och övergångar som leder till spelarens mål.

Ett namn får aldrig vara nödvändigt för att ett spår ska kunna användas. Ett namn får börja eller sluta mitt på en fysisk spårsektion och kan fortsätta genom flera korsningar.

### 2.2 Interna ID:n visas inte för spelaren

Alla objekt har stabila tekniska ID:n. Visningsnamn är valfria där de inte behövs för spelupplevelsen. Tekniska namn som `section_0042` eller `track_a81f` ska inte visas i vanlig HUD.

### 2.3 Spelaren ska aldrig behöva förstå datamodellen

Administratören ska kunna bygga och granska nätet visuellt. Vanliga spelare ska endast se relevanta namn, instruktioner och den aktiva resan.

### 2.4 Osäkerhet ska visas, inte döljas

När två spår inte kan särskiljas säkert ska HUD visa den valda **destinationen** och **Osäker position**. Den ska **dölja alla sväng-/knappinstruktioner**, inklusive gamla instruktioner, tills rätt spår och riktning åter är säkert identifierade. Samma princip gäller oklara eller icke verifierade vägval.

## 3. Begrepp

| Begrepp | Betydelse |
|---|---|
| **Kontrollpunkt** | Intern teknisk referens till en station eller ett rödljus; ingen ytterligare platstyp behöver visas för spelaren. |
| **Zon** | Ett tredimensionellt område bestående av en eller flera boxar. |
| **Station** | Namngivet stoppställe på rälsen, vid spårslut eller på ett genomgående spår; inget krav på plattformar. |
| **Korsning/rödljus** | Kontrollpunkt där spår möts och vissa övergångar kan väljas. |
| **Port** | En fysisk in- eller utgång där ett spår passerar kontrollpunktens gräns. |
| **Spårsektion** | Fysisk korridor mellan två närliggande kontrollpunkter. |
| **Fysiskt spår** | En inspelad rälsgeometri inom en sektion. En sektion kan innehålla flera parallella spår. |
| **Passage** | Användning av ett fysiskt spår i en tillåten riktning. |
| **Övergång** | Tillåten koppling från en inkommande passage till en utgående passage vid en kontrollpunkt. |
| **Namnlager** | Ett valfritt namn som läggs över hela eller delar av ett eller flera spår, exempelvis Riksettan eller Centralbron. |
| **Resväg** | Den väg GPS:en beräknar som en följd av passager och övergångar. |
| **Inspelning/survey** | En inspelningsbehörig användares observerade minecart-färd som används för att registrera fysisk geometri och genomförda övergångar. |

## 4. Systemarkitektur

### 4.1 Serverdel

Servern ansvarar för att:

1. Lagra den auktoritativa järnvägsmodellen.
2. Hantera kontrollpunkter, zoner, spårsektioner, fysiska spår, övergångar och namnlager.
3. Spela in minecart-färder för personer med inspelningsbehörighet.
4. Skapa förslag på sektioner, spårpar och observerade övergångar.
5. Validera alla ändringar innan de publiceras.
6. Bygga en riktad navigationsgraf.
7. Distribuera versionsmärkta nätverkssnapshots och ändringar till kompatibla klienter.
8. Separat behörighetskontrollera navigation, inspektion, namngivning, inspelning och administration/publicering.

Serverdelen får inte kräva JourneyMap eller ladda klientklasser på en dedikerad server.

### 4.2 Klientdel

Klienten ansvarar för att:

1. Ta emot och cachelagra serverns aktuella nätverksvy.
2. Låta spelaren söka efter och välja destination.
3. Beräkna eller följa servergodkänd resväg.
4. Matcha minecartens position och riktning mot rätt fysiskt spår.
5. Visa HUD, aktiv resväg och manöverinstruktioner.
6. Visa ett inbyggt visuellt nätverksläge.
7. Ge användare med lämpliga separata inspektions-, namn-, inspelnings- och adminbehörigheter deras respektive verktyg.
8. Valfritt spegla samma vy i JourneyMap.

Spelare utan Railway GPS ska kunna ansluta och spela normalt. Servern skickar endast modspecifika data till kompatibla klienter.

## 5. Domänmodell

### 5.1 Zoner

En zon är dimensionsbunden och består av en eller flera axis-aligned bounding boxes. Rödljus kan ha en sammansatt zon för att identifiera infart och utfart. **Stationer behöver bara en enkel stoppunkt eller stoppzon på rätt räls**; inget komplext stationsområde eller plattformssystem krävs.

```json
{
  "dimension": "minecraft:overworld",
  "shapes": [
    { "min": [10, 70, 30], "max": [28, 78, 55] },
    { "min": [28, 70, 38], "max": [45, 78, 47] }
  ],
  "enterMargin": 0.0,
  "exitMargin": 1.5
}
```

Krav:

- `Y` ingår alltid så att broar, tunnlar och staplade spår kan skiljas åt.
- Inträde och utträde använder hysteres för att undvika fladdrande tillstånd vid zonkanten.
- Flera sammanhängande ticks kan krävas innan inträde eller utträde bekräftas.
- Överlappande kontrollzoner varnas och kräver uttrycklig bekräftelse.
- Zoner kan visualiseras och redigeras på klienten av behöriga administratörer.

### 5.2 Stationer

En **station** är en namngiven punkt eller liten stoppzon **direkt på en fysisk räls**. Det kan vara ett spårslut eller ett genomgående spår där minecarten kan stanna och sedan fortsätta eller vända. Stationen kräver **inte** någon plattform, stationsbyggnad, flera spår eller ett stort stationsområde. **Ingen separat terminaltyp används:** ett spårslut är också en station.

Varje station har bara det som behövs för navigation: namn, tekniskt ID, dimension samt en stoppunkt/stoppzon kopplad till **rätt fysisk räls**. Position och rälsidentitet ska skilja den från en eventuell närliggande parallell räls.

**Endast stationer är valbara destinationer.** GPS:en markerar **Framme** så snart minecarten med säker färdhistorik når rätt station/stoppzon på rätt spår; ett fullständigt stopp eller låg hastighet krävs inte. När **Framme** väl har bekräftats ska texten ligga kvar även om vagnen fortsätter röra sig. **När spelaren kliver ur minecarten avslutas navigeringen automatiskt och HUD:en försvinner.** En förbipassage på fel spår får inte utlösa ankomst.

### 5.3 Korsningar och rödljus

En korsning består av:

- tekniskt ID;
- valfritt visningsnamn;
- en kontrollzon;
- portar där spår går in i eller ut ur zonen;
- uttryckligen tillåtna övergångar;
- valfria instruktioner för knappar eller andra kontroller.

Kontrollzonen omsluter hela området där vägvalet sker. Den används för att dela inspelningar, identifiera inträde och utträde och bekräfta vilket vägval vagnen faktiskt genomförde.

Zonen bestämmer inte automatiskt vilka svängar som är möjliga. Det gör övergångarna. **Rödljus är black boxes:** spår spelas bara in fram till/ifrån zongränsens portar; interna rälsbitar, växelmekanik och redstone modelleras inte. En inspelad och säkerställd infart→utfart kan registreras som giltig övergång utan separat manuell publiceringsbekräftelse. Oklara vägval förblir overifierade tills de kan verifieras eller administratören bekräftar dem. Stationer är de enda stopp-/ankomstobjekten; rödljus är enbart vägval.

### 5.4 Spårsektioner och fysiska spår

En spårsektion förbinder två närliggande kontrollpunkter och grupperar de fysiska spår som hör till samma korridor.

```text
Spårsektion: västra infarten
├── spår A: väst → öst
├── spår B: öst → väst
└── eventuellt reservspår
```

Varje fysiskt spår innehåller:

- stabilt tekniskt ID;
- sektionstillhörighet;
- två portar/ändpunkter;
- inspelad 3D-geometri;
- faktisk längd;
- tillåtna riktningar;
- status, exempelvis utkast, verifierat eller avstängt.

Följande måste stödjas:

- **Tvåspårigt:** separat geometri för vardera riktningen.
- **Enkelspårigt dubbelriktat:** samma geometri kan traverseras i båda riktningarna.
- **Enkelriktat:** endast en passage är tillåten.
- **Fler än två spår:** exempelvis extra eller parallella fysiska rälslinjer, utan krav på plattformar.

Två parallella spår ska inte representeras genom att samma linje vänds baklänges om de i världen ligger på olika räls.

### 5.5 Passager och övergångar

En passage är ett fysiskt spår plus en riktning. Ruttgrafen arbetar med passager, inte bara med sektioner.

En övergång beskriver ett verifierat vägval:

```text
inkommande passage → kontrollpunkt → utgående passage
```

Övergången kan innehålla:

- tillåten/avstängd status;
- verifierad identifiering av infarts- och utfartsport, **utan att lagra intern rälsgeometri**;
- kontroll-/knappinstruktion;
- ordagrann administratörsskriven instruktion (inte automatiskt översatt);
- källa, exempelvis observerad inspelning eller administratörsbeslut.

Att två spår möter samma kontrollzon betyder inte att övergången mellan dem automatiskt är möjlig.

### 5.6 Namnlager

Namnlager är presentation ovanpå den fysiska modellen. Ett namnlager kan omfatta hela eller partiella intervall av flera spår.

Typer kan exempelvis vara:

- `way` – längre led, exempelvis Riksettan;
- `bridge` – bro, exempelvis Centralbron;
- `tunnel` – tunnel;
- `district` – område;
- `custom` – serverdefinierad typ.

Ett namnlager innehåller:

- tekniskt ID;
- visningsnamn;
- typ;
- prioritet;
- valfri färg och ikon;
- en mängd spårintervall.

```json
{
  "id": "centralbron",
  "name": "Centralbron",
  "type": "bridge",
  "priority": 50,
  "ranges": [
    { "track": "track_eastbound", "from": 42.0, "to": 118.0 },
    { "track": "track_westbound", "from": 40.0, "to": 116.0 }
  ]
}
```

Konsekvenser:

- Centralbron kan börja och sluta vid visuella markeringar mitt på ett spår utan nya grafnoder.
- Riksettan kan fortsätta genom flera spårsektioner och rödljus.
- Samma fysiska spår kan tillhöra flera namn, exempelvis både Riksettan och Centralbron.
- Namnlösa spår fungerar fullt ut men ger ingen påhittad etikett i HUD.
- Namn påverkar inte vilka resvägar som är fysiskt möjliga.

### 5.7 Resväg

En resväg lagras inte som en duplicerad helhetsgeometri. Den beräknas som en följd av passager och övergångar genom det gemensamma fysiska nätet.

Exempel:

```text
Station A
→ namnlös anslutning
→ Riksettan
→ Riksettan · Centralbron
→ Riksettan
→ stationsinfart
→ Station B
```

Flera resor som passerar Centralbron använder samma registrerade spår och samma namnlager. Bron behöver inte spelas in på nytt för varje destinationskombination.

## 6. Serverlagring

Den auktoritativa modellen bör lagras transaktionellt med schemaversion, exempelvis i en världslokal SQLite-databas:

```text
<världsmapp>/railwaygps/network.db
```

Rekommenderade logiska tabeller/samlingar:

```text
control_points
zones
zone_boxes
stations
platforms
junctions
ports
track_sections
physical_tracks
track_geometry
allowed_transitions
custom_instructions
labels
label_track_ranges
network_revisions
```

Krav:

- Ändringar sker i transaktioner och blir aktiva först efter fullständig validering.
- Den senast fungerande revisionen behålls om en ändring misslyckas.
- Automatiska säkerhetskopior skapas före destruktiva eller större operationer.
- JSON-export och JSON-import ska finnas för granskning, backup och externa verktyg.
- Importerad data aktiveras aldrig innan hela modellen validerats.
- ID:n valideras och användarinmatning får inte kunna skriva utanför världens Railway GPS-data.
- Databasen är en implementationdetalj; klienter får en kompakt nätverksmodell, inte direkt databasåtkomst.

## 7. Administration och redigering

Behörigheter ska vara **separata och serverkontrollerade**. Vanlig navigation kräver ingen redigeringsroll. Fyra oberoende rättigheter används:

| Rättighet | Tillåten åtgärd |
|---|---|
| **Inspektion** | Se teknisk nätverksinformation, status och felsökningsverktyg; inte ändra data. |
| **Namngivning** | Föreslå/skapa/ändra namnlager på befintliga verifierade spår; inte ändra fysisk topologi. |
| **Inspelning** | Starta/avsluta/avbryta minecart-surveys; giltigt `finish` publicerar automatiskt det inspelade nätverkstillskottet enligt serverns policy. |
| **Administration/publicering** | Skapa/ändra kontrollpunkter, portar, spår, tillåtna övergångar och rättigheter; bekräfta spårpar, importera/återställa revisioner och utföra destruktiva ingrepp. |

Servern kontrollerar varje begäran, även via klient-GUI och API. Klienterna får bara data de har rätt att se. Rollernas exakta kommandon/behörighetsnoder är en teknisk detalj, men separationen är beslutad. En användare kan ha flera rättigheter; en ren inspelare behöver inte moderatorbehörighet. Spårparning kräver uttryckligt godkännande med administrations-/publiceringsrätt.

Vanliga spelare ska bara kunna söka fram och välja **stationer** som destinationer. Spårslut hanteras som vanliga stationer, inte som en extra terminaltyp.

### 7.1 Visuellt markeringsverktyg

Administratören ska kunna markera två hörn i världen med ett verktyg. Klienten visar omedelbart:

- valda hörn;
- transparent volym och kantlinjer;
- zonens storlek;
- spår som träffas av zonen;
- överlapp med befintliga zoner;
- varningar innan zonen bekräftas.

Flera boxar kan läggas till samma zon. JourneyMap eller nätverksinspektören kan användas som kompletterande redigeringsyta.

### 7.2 Skapa en korsning

Arbetsflödet är:

1. Skapa korsningsobjektet.
2. Markera kontrollzonen runt hela växel-/rödljusområdet.
3. Spela in spåren som passerar zonen.
4. Låt moddet föreslå portar vid zonens gräns.
5. Verifiera observerade möjliga övergångar (administratörsbeslut behövs om observationen är osäker).
6. Lägg vid behov till kontrollinstruktioner.

Illustrativ kommandodesign:

```mcfunction
/rgps junction create sigma
/rgps junction zone select sigma
/rgps zone add-box
/rgps zone confirm
```

### 7.3 Skapa en station

1. Ange ett namn på stoppstället.
2. Markera en enkel **stoppunkt/stoppzon direkt på rälsen**, vid spårslut eller längs ett genomgående spår.
3. Identifiera rätt fysisk räls för att undvika falsk ankomst på ett närliggande parallellt spår.
4. Spara med behörighet för administration/publicering.

Inga plattformar, stationsområden eller separata terminaler behöver skapas.

### 7.4 Spela in fysisk räls

En användare med **inspelningsbehörighet** får starta en lång survey **var som helst på verklig räls**, även mellan stationer och rödljus och utanför registrerade zoner. Start i en kontrollpunktszon krävs **inte**. Inspelningen får passera flera rödljus utan omstart. **Före publicering vid `finish` måste systemet säkert kunna koppla hela inspelningen till det registrerade nätet**, inklusive start-/slutanslutningar och rätt fysisk räls/riktning. Om det inte går ska hela inspelningen avvisas, utan att skapa påhittade kontrollpunkter.

```mcfunction
/rgps survey start
/rgps survey status
/rgps survey finish
/rgps survey cancel
```

Vid inspelning registreras **fysisk räls utanför rödljusens zoner**. Inspelning som startar eller slutar mitt på en tidigare känd spårsträcka får matchas till just den rälsen utan att skapa extra platsobjekt eller duplicerad geometri. Vid zongränsen identifieras infartsporten, intern rälsgeometri ignoreras och nästa inspelade fysiska spår börjar vid utfartsporten. Observerade och entydigt verifierade infart→utfart-övergångar kan ingå i samma publikation. Stationer har sina enkla stoppunkter eller stoppzoner på rälsen.

**Beslutad publiceringsregel:** `/rgps survey finish` analyserar och validerar **hela inspelningen** och publicerar alla giltiga tillskott **direkt, atomiskt och i en revision**, utan ett obligatoriskt separat `confirm`-/`discard`-steg. Vid kritiskt fel, osäker koppling som kräver manuell kontroll eller misslyckad skrivning publiceras **ingenting**; användaren får en begriplig felrapport. Survey-avbrytning får inte spara delvis data. Visuell förhandsvisning och diagnoser är frivilligt stöd, inte ett extra publiceringskrav. Administratörsåtgärder för senare redigering/återställning är en separat process.

**Spårpar är ett undantag från automatiken:** vid `finish` får moddet föreslå geometriskt närliggande motriktade spårpar men får **aldrig para ihop dem automatiskt**, även om exakt en kandidat hittas. Ett förslag kräver efterföljande uttryckligt godkännande av person med behörigheten administration/publicering. Inspelade spår kan ändå publiceras oparade om de är giltiga. När redan existerande spår upptäcks får systemet inte destruktivt slå samman dem automatiskt.

### 7.5 Tvåspåriga sträckor

Vardera fysiska rälsen spelas in separat. Moddet **föreslår par utifrån fysisk närhet, riktning och geometrisk överensstämmelse**; det räcker inte att spåren har omvända kontrollpunkter. Förslaget visar vilka två spår det gäller och osäkerhet. **Ingen parning sker automatiskt**, inte ens med en enda kandidat. En person med administrations-/publiceringsrätt måste godkänna paret uttryckligen. Oparade, giltiga spår fungerar i nätet.

Parningen påverkar presentation och administration. Ruttberäkningen fortsätter att använda respektive riktad passage.

### 7.6 Namnge en känd sträcka mellan två punkter

När de fysiska spåren redan är registrerade ska en behörig person kunna namnge en sträcka utan att åka den igen.

Arbetsflödet är:

1. Välj typ, tekniskt ID och visningsnamn för namnet.
2. Markera en startpunkt och en slutpunkt i världen eller på kartan.
3. Moddet projicerar punkterna på närmaste kompatibla registrerade spår.
4. Moddet räknar fram sammanhängande kandidater genom det kända spårnätet.
5. Kandidaten förhandsvisas tydligt i världen och på kartan.
6. Personen väljer om namnet ska gälla vald riktning, hela spårsektioner eller även parade motriktade spår.
7. Namnet sparas först efter uttrycklig bekräftelse.

Första och sista spåret får partiella intervall. Namngivningen skapar därför inga kontrollpunkter och förändrar inte ruttgrafen.

Illustrativt flöde:

```mcfunction
/rgps label select start way riksettan Riksettan
# Klicka startpunkt och slutpunkt med markeringsverktyget
/rgps label include paired-tracks
/rgps label confirm
```

Om endast en sammanhängande väg finns mellan punkterna kan den väljas automatiskt för förhandsvisning. Om flera vägar är möjliga måste moddet visa alternativen och kräva ett val. Personen ska också kunna lägga till en eller flera mellanpunkter för att styra sträckan genom rätt gren:

```mcfunction
/rgps label add-via
```

En namngivning får aldrig tyst välja en godtycklig gren. Punkten ska inte heller fästas på ett närliggande parallellt spår utan att den visuella förhandsvisningen visar exakt vad som valts.

Att köra sträckan med minecart finns kvar som ett alternativ när nätet ännu inte är registrerat, när en komplex väg är enklare att demonstrera genom körning eller när personen uttryckligen föredrar det.

### 7.7 Namnge Riksettan

När Riksettans fysiska spår redan finns markerar personen start och slut, granskar den beräknade vägen och inkluderar vid behov de parade spåren. Namnet kan på detta sätt omfatta många sektioner och kontrollpunkter utan en ny minecart-färd.

En längre led kan namnges genom att köra längs redan registrerade spår:

```mcfunction
/rgps label record start way riksettan Riksettan
/rgps label record finish
```

Moddet matchar färden mot de fysiska spåren och lägger namnet över alla berörda intervall. Motsvarande parallella spår kan inkluderas efter förhandsvisning och bekräftelse.

Riksettan kan i båda fallen fortsätta genom valfritt antal rödljus eftersom namnet inte är en grafkant.

### 7.8 Namnge Centralbron

Om spåren redan är registrerade markerar personen de två visuella gränserna, exempelvis stockarna på vardera sidan. Moddet väljer intervallet mellan de projicerade punkterna och kan efter bekräftelse lägga samma namn på det parade motriktade spåret.

Som alternativ kan personen starta en namninspelning vid den ena gränsen och avsluta vid den andra:

```mcfunction
/rgps label record start bridge centralbron Centralbron
/rgps label record finish
```

Resultatet är spårintervall som motsvarar exakt den markerade delen. Inga falska korsningsnoder behövs.

### 7.9 Anpassade instruktioner

En instruktion kopplas till en utgång eller en specifik inkommande–utgående övergång vid en kontrollpunkt.

```text
Kontrollpunkt: Sigma
Utgående passage: Martyrögrenen
Instruktion: Välj knappen för Martyrön
```

En generell instruktion för utgången kan kompletteras med mer specifika övergångsregler. Den mest specifika giltiga regeln vinner. **Administratörsskrivna anpassade instruktioner visas ordagrant**, på exakt det språk och med den text administratören angav, **oavsett spelarens språk**. Ingen översättningsvariant krävs. Vid **Osäker position** visas inte denna eller andra sväng-/knappinstruktioner.

## 8. Tekniskt inspelningsbeteende

Inspelningen sker på serversidan och kopplas till den **inspelningsbehöriga** användaren och den specifika minecart som används vid start.

### 8.1 Insamling

- Sampla minecartens position och rörelse under server-ticks.
- Identifiera rälsblocket som vagnen följer när detta kan göras säkert.
- Registrera ordning, kurvor, lutningar och dimension.
- Undvik duplicerade punkter när vagnen står stilla.
- Registrera passage genom kontrollzoner med tidsordning och riktning. Rödljusens inre är **opaque**: logga infarts-/utfartsport och observerat vägval, inte intern rälsgeometri.
- Tillåt **survey-start var som helst på verklig räls**, även utan registrerad zon vid start. Kontrollera säkert mot nätets befintliga fysiska spår före publicering.
- Visa den inspelade linjen live på administratörens klient.

### 8.2 Efterbearbetning

- Beräkna faktisk färdlängd längs spåret.
- Förenkla raka delar utan att förlora kurvor, höjdskillnader eller parallell separation.
- Projektera kontrollzonernas gränspassager på geometrin.
- Identifiera befintlig geometri som den nya inspelningen sannolikt motsvarar, **även för en start mitt på ett spår**; saknas säker anslutning till nätet får hela inspelningen inte publiceras.
- Skapa förslag, aldrig tysta destruktiva sammanslagningar eller automatisk spårparning. Entydigt giltiga inspelningar publiceras vid `finish` utan manuellt publiceringssteg; osäkra inspelningar avvisas helt i stället för att aktiveras delvis.

### 8.3 Strikt avbrott

Om den inspelningsbehöriga användaren lämnar minecarten avbryts inspelningen omedelbart och all tillfällig data kastas. Samma regel gäller vid:

- frånkoppling;
- död;
- dimensionsbyte;
- förstörd minecart;
- byte till en annan minecart.

Manuell avbrytning:

```mcfunction
/rgps survey cancel
```

Att nå en station eller kontrollzon får ge ett förslag om att avsluta, men ska inte automatiskt spara eller publicera inspelningen.

## 9. Navigationsgraf och ruttberäkning

Grafens tillstånd bör vara mer detaljerat än en nod per korsning. Ett lämpligt internt tillstånd är en port eller spårände vid en kontrollpunkt.

Grafkanter är:

1. **Passagekanter:** färd längs ett fysiskt spår i tillåten riktning.
2. **Övergångskanter:** verifierad rörelse genom en kontrollpunkt från en passage till en annan.

Detta förhindrar att alla spår vid samma korsning automatiskt betraktas som ömsesidigt anslutna.

Ruttberäkningen använder **längden på den verifierade fysiska rälsen utanför rödljuszoner**. Ett rödljusbidrag är **noll**: ingen uppskattad väntetid, manuell korsningskostnad, övergångsvikt eller extra straffkostnad behövs. Avstängda spår och övergångar är **otillåtna**, inte dyrare alternativ.

Dijkstra eller A* kan användas. Ruttresultatet är en följd av specifika riktade passager och övergångar, vilket gör att klienten vet exakt vilket av två parallella spår som avses.

## 10. Positionsbestämning

Klienten gör kontinuerlig map matching med:

- spelarens/minecartens 3D-position;
- färdriktning och hastighet;
- föregående bekräftade passage;
- planerad nästa passage;
- kontrollzonernas inträde och utträde;
- avstånd längs kandidaternas geometri.

Färdriktning och färdhistorik ska väga tungt så att två parallella spår inte sammanblandas.

Tillstånd kan vara:

```text
På bekräftat spår
→ Närmar sig kontrollpunkt
→ Inne i kontrollzon
→ Utgående spår observerat
→ Ny passage bekräftad
```

Om spelaren tar en annan verifierad övergång än den planerade räknas resvägen om. Om läget är tvetydigt visas **destinationen och Osäker position**, medan alla vägvals-/sväng-/knappinstruktioner döljs tills rätt spår kan identifieras. Efter bekräftad ankomst visas **Framme** fram till avstigning; då stoppas navigationen.

## 11. Navigationsinstruktioner och HUD

HUD:en ska prioritera handling framför tekniska detaljer:

```text
RAILWAY GPS · Central
RIKSETTAN · Centralbron

Välj knappen för Martyrön
Nästa kontrollpunkt: Sigma · 28 block

Återstående: 1 240 block
```

### 11.1 Automatisk manöver

Den sista stabila vektorn på inkommande spår jämförs med den första stabila vektorn på utgående spår. Resultatet klassificeras som:

- rakt fram;
- vänster;
- höger;
- tillbaka;
- osäkert.

Geometrin ska hämtas från den verkliga inspelade rälsen, inte från raka linjer mellan kontrollpunkternas mittpunkter.

### 11.2 Instruktionsprioritet

1. Specifik manuell instruktion för exakt övergång.
2. Generell kontrollinstruktion för den valda utgången.
3. Automatisk vänster/höger/rakt/tillbaka-instruktion.
4. Neutral instruktion mot nästa relevanta mål **bara när rätt spår är känt** men manöverns riktning inte kan avgöras. Vid **Osäker position** visas **ingen sväng-, vägvals- eller knappinstruktion**, enbart destinationen och osäkerhetsstatus.

### 11.3 Namnpresentation

- HUD visar **högst en huvudled och högst ett lokalt namn** (t.ex. bro/tunnel) samtidigt: `Riksettan · Centralbron`. Huvudled är primärt `way` och lokalt namn en närliggande namngiven konstruktion; vid flera kandidater väljs högsta namnlagerprioritet inom respektive kategori. Saknas en kategori visas enbart den andra. Övriga namn kan visas i karta/inspektör.
- Om spelaren fortsätter inom samma led kan HUD visa `Fortsätt på Riksettan`.
- Vid namnbyte kan den visa `Fortsätt åt vänster in på Hamnleden`.
- Namnlösa anslutningar visar inget internt ID; HUD använder nästa station eller kontrollpunkt som kontext.
- Alla **inbyggda standardtexter** ska vara lokaliserbara. **Administratörsskrivna instruktioner visas exakt som de skrevs** oavsett spelarens språk.

## 12. Inbyggd visuell nätverksvy

Den visuella nätverksinspektören är en kärnfunktion och ska fungera utan JourneyMap.

### 12.1 Visningslägen

Ett tangentkommando växlar mellan:

- **Av:** endast aktiv navigering visas.
- **Enkel:** stationer, namngivna objekt och aktiv resväg.
- **Nätverk:** alla registrerade spår, kontrollpunkter och namn.
- **Administration:** zoner, portar, riktningar, övergångar, utkast och valideringsfel.

Administrationsläget kräver behörighet.

### 12.2 Världsrendering

Klienten ska kunna visa:

- stationers enkla stoppunkter eller stoppzoner på rälsen;
- korsningars kontrollzoner;
- inspelade spår som linjer ovanför rälsen;
- riktningspilar;
- tillåtna övergångar genom korsningar;
- namnlager och deras exakta intervall;
- aktiv resväg och valt fysiskt spår;
- pågående inspelning;
- utkast och felaktig geometri.

Föreslagna standarder:

| Objekt | Standardvisning |
|---|---|
| Namnlöst spår | Grå linje |
| Namngiven led | Ledens färg |
| Aktiv resväg | Kraftig blå linje |
| Station | Cyan zon |
| Stationens stoppunkt | Grön markering |
| Korsning | Orange zon |
| Otillåten övergång | Rött kryss |
| Fel eller lös geometri | Röd markerad linje |
| Pågående inspelning | Gul linje |

Färger, opacitet och linjemönster ska kunna anpassas och inte vara den enda informationsbäraren.

### 12.3 Nätverksutforskare

Klienten ska ha en sökbar panel:

```text
Nätverk
├── Leder
│   └── Riksettan
├── Konstruktioner
│   └── Centralbron
├── Stationer
│   └── Centralstationen
├── Korsningar
│   └── Sigma
└── Namnlösa spår
```

När ett objekt väljs ska det kunna:

- markeras i världen och på kartan;
- centreras i en kartvy;
- visa anslutna objekt;
- visa spår och riktningar;
- visa tillåtna övergångar och instruktioner;
- visa datakälla, revision och valideringsstatus;
- visa tekniska ID:n endast i administratörsläge.

### 12.4 Visuell inspelning och redigering

Railway GPS öppnas med en konfigurerbar tangent (`R` som standard). Vanliga spelare får en sökbar
destinationslista med **enbart stationer** (spårslut ingår som stationer; rödljus är aldrig destinationer) samt tydliga knappar för att
starta eller stoppa navigeringen. Tekniska ID:n behöver inte användas.

Behöriga användare får de **inspektions-, namn-, inspelnings- och administrationsverktyg** som deras separata serverrättigheter tillåter. Administratörer får dessutom knappen **Adminverktyg**. Den öppnar redigeringsläget, som visar
aktuella musåtgärder och vald geometri i en HUD, i samma anda som verktygspaletten i SimpleBlueprints. För
kontrollzoner väljer vänsterklick första hörnet och högerklick andra hörnet; en tredimensionell kontur visas
omedelbart. `R` lämnar adminläget. Servern kontrollerar fortfarande behörighet och äger den sparade modellen.

Under inspelning kan geometrin visas live och in- och utträde ur kontrollzoner markeras. Giltig inspelning publiceras **direkt** vid `finish` efter full validering; inga obligatoriska granskningsutkast eller separat `survey confirm` krävs. Geometriska **spårparningsförslag** visas separat och måste bekräftas av behörig administratör innan parning kan sparas.

Vid punktbaserad namngivning ska startpunkt, slutpunkt, eventuella mellanpunkter och hela den beräknade spårvägen visas. Om flera kandidater finns ska användaren kunna växla mellan dem innan bekräftelse. Vald riktning, partiella ändintervall och automatiskt inkluderade parallella spår ska gå att särskilja visuellt.

En behörig redaktör ska kunna se vad servern tolkat utan att läsa databas- eller JSON-filer.

## 13. JourneyMap-integration

JourneyMap är valfri och använder samma presentationsmodell som den inbyggda inspektören.

Önskade funktioner:

- visa båda fysiska spåren separat;
- visa riktningspilar eller riktad stil;
- visa namngivna leder sammanhängande genom flera kontrollpunkter;
- visa Centralbron endast över dess registrerade intervall;
- visa stationernas enkla stoppunkter eller stoppzoner på rälsen;
- markera den aktiva passagen starkare än övriga spår;
- visa kontrollzoner och fel endast i administrationsläge;
- låta behöriga redaktörer markera två punkter, förhandsgranska spårvägen mellan dem och skapa ett namnlager;
- låta spelaren välja destination via stationer på kartan när API:t tillåter det.

Railway GPS äger alltid grafen, namnen och navigeringen. JourneyMap är endast visning och möjlig inmatning.

## 14. Synkronisering

Föreslaget anslutningsflöde:

1. Servern laddar och validerar senaste aktiva revision.
2. En kompatibel klient annonserar protokollstöd; **servern bestämmer själv** användarens separata inspektions-, namn-, inspelnings- och administrations-/publiceringsrättigheter och filtrerar snapshots därefter.
3. Servern skickar en kompakt snapshot med nätverksversion/hash.
4. Klienten bygger rumsliga index och renderingscache.
5. Senare ändringar skickas som revisionerade deltauppdateringar eller ny snapshot vid behov.

Krav:

- Paket ska storleksbegränsas och vid behov delas upp.
- Klienten aktiverar inte en ofullständig revision.
- Borttagna eller ändrade passager orsakar omplanering av aktiv resa.
- Administrationsmetadata skickas endast till behöriga klienter när den inte behövs för vanlig navigering.
- Servern förblir auktoritativ även om klienten visar eller föreslår redigeringar.

## 15. Validering och säkerhet

Nätverket får inte publiceras om kritiska fel finns.

Valideringen ska bland annat hitta:

- spår utan giltiga ändpunkter;
- kontrollzoner utan portar;
- portar utan anslutna spår;
- övergångar som refererar till saknade passager;
- stationers stoppunkter/stoppzoner utan säker koppling till rätt fysisk räls;
- oavsiktligt överlappande kontrollzoner;
- geometri som hoppar orimligt långt;
- spår som byter dimension utan särskild övergång;
- namnlager med ogiltiga intervall;
- dubbletter och trasiga ID-referenser;
- oavsiktlig sammanblandning av spår på olika höjd;
- aktiva resvägar som använder avstängda objekt.

Servern upprätthåller **separata behörigheter för inspektion, namngivning, inspelning och administration/publicering**. Inspelningsbehöriga får publicera *endast ett fullständigt validerat survey-resultat* direkt genom `finish`; övrig skapelse/ändring av fysisk topologi och spårparsbekräftelse kräver administrations-/publiceringsrätt. Namnredaktörer får inte ändra fysiska spår eller rödljusövergångar. Teknisk/adminspecifik information ges bara till behöriga mottagare; normal navigation är öppen för spelare med kompatibel klient.

Destruktiva ändringar kräver förhandsvisning, beroendeanalys och bekräftelse. Systemet ska kunna återgå till föregående fungerande revision.

## 16. Prestanda

Stora nätverk ska hanteras med:

- rumsligt index per dimension och chunk;
- geometrisk förenkling i flera detaljnivåer;
- avståndsbegränsad världsrendering;
- cache av färdiga renderingssegment;
- separata lager som kan stängas av;
- inkrementella nätverksuppdateringar;
- begränsad kandidatmängd vid map matching;
- bakgrundsberäkning där Minecrafts trådsäkerhetsregler tillåter det.

Full detalj behövs nära spelaren och den aktiva resvägen. Avlägsna spår kan visas förenklat.

## 17. Exempelscenario

Följande nät innehåller två parallella spår, flera rödljus och en kort bro:

```text
                         Riksettan
┌──────────────────────────────────────────────────────────────┐
Station A ══ Signal 1 ══ Signal 2 ══ Centralbron ══ Signal 3 ══ Station B
└──────────────────────────────────────────────────────────────┘
```

Den fysiska modellen består av flera sektioner mellan stationerna och signalerna. Varje sektion innehåller ett östgående och ett västgående spår.

Namnet **Riksettan** täcker samtliga relevanta spårintervall och bryts inte av signalerna. Namnet **Centralbron** täcker endast delen mellan brons visuella gränser på båda spåren.

En resa från Station A till Station B använder de östgående passagerna. En resa åt andra hållet använder de separat inspelade västgående passagerna. Båda visar Riksettan, och endast medan vagnen befinner sig på bron visas Centralbron.

Vid Signal 2 kan övergången exempelvis ha instruktionen:

```text
Välj knappen för Martyrön
```

om den planerade utgången kräver ett byggnadsspecifikt knappval. I annat fall beräknas riktningen från spårgeometrin.

## 18. Prioriterad utvecklingsplan

### Fas 1 – Domänmodell och lagring

- Kontrollpunkter, sammansatta 3D-zoner och portar.
- Enkla stationer som stoppunkter eller små stoppzoner direkt på rätt räls; inget plattformssystem.
- Spårsektioner, fysiska spår, passager och övergångar.
- Transaktionell, revisionshanterad lagring.
- Full nätverksvalidering och JSON-export/import.

### Fas 2 – Synkronisering och grundvisualisering

- Server–klient-snapshot med version/hash.
- Inbyggd rendering av zoner, spår och riktningar.
- Visningslägen och behörighetsstyrd administratörsvy.
- Grundläggande nätverksutforskare.

### Fas 3 – Visuell zonredigering

- Markering av två hörn i världen.
- Flera boxar per zon.
- Förhandsvisning, mått och överlappsvarningar.
- Stationers enkla stoppunkter och rödljusens kontrollzoner.

### Fas 4 – Survey och fysisk nätbyggnad

- Serversidig minecart-inspelning.
- Strikt avbrott vid dismount eller annan sessionsförlust.
- Långa inspelningar genom flera kontrollzoner.
- Automatiska delnings-, port- och övergångsförslag.
- **Automatisk, atomisk publicering av helt validerade surveys vid `finish`**, med total avvisning vid fel; separat granskningspublicering krävs inte.

### Fas 5 – Flerspårighet och övergångar

- Inspelning av separata riktningar.
- **Geometribaserade förslag på parallella spårpar och obligatoriskt uttryckligt administratörsgodkännande**; inga automatiska parningar.
- Tillåtna/otillåtna korsningsövergångar.
- Kontrollinstruktioner för knappar och specialbyggen.

### Fas 6 – Namnlager

- Långa leder genom flera sektioner, exempelvis Riksettan.
- Partiella lokala intervall, exempelvis Centralbron.
- Flera samtidiga namn på samma spår.
- Punkt-till-punkt-namngivning över redan registrerade spår.
- Kandidatval, mellanpunkter och inkludering av parade spår.
- Namninspelning och visuell intervallredigering som alternativ.

### Fas 7 – Navigation

- Ruttberäkning över passager och verifierade övergångar.
- Navigering får startas var som helst på en registrerad fysisk räls. Vagnen matchas först mot exakt fysisk
  räls och tillåten färdriktning; en parallell grannräls får inte väljas bara för att den har en giltig rutt.
- Stabil map matching mellan parallella spår.
- Omplanering och osäkerhetsläge.
- Automatiska och manuella instruktioner.
- Startinstruktionen beräknas från den fysiska inkommande och utgående rälsen i kontrollzonen. Spelarens
  kamerariktning får aldrig användas för att avgöra vänster eller höger; saknas säker rälsmatchning visas en
  neutral fortsätt-instruktion.
- Lokaliserad HUD med **bokstavligt återgivna anpassade instruktioner**, **högst en huvudled plus ett lokalt namn**, och osäkerhetsstatus **utan sväng-/knappinstruktioner**.
- **Sökning och destinationsval endast för stationer; bekräftad ankomst vid rätt räls/stoppzon även under rörelse; behåll `Framme` tills avstigning och avsluta då navigeringen automatiskt.**

### Fas 8 – JourneyMap och administrationspolering

- Gemensam presentationsmodell för intern rendering och JourneyMap.
- Kartbaserat destinationsval **endast för stationer**.
- Sökning, filtrering och objektinspektion.
- Prestandaoptimering för stora nät.

## 19. Acceptanskriterier

En första komplett version av målarkitekturen ska demonstrera att:

1. Två separata parallella spår mellan samma kontrollpunkter kan spelas in och navigeras i rätt riktning.
2. Ett enkelspår kan markeras som verifierat dubbelriktat utan duplicerad geometri.
3. En lång inspelning genom flera rödljus delas i yttre spår och verifierade black-box-övergångar, och **publiceras atomiskt direkt vid giltigt `finish`** utan separat obligatorisk granskning; vägnamnet behöver inte brytas.
4. Riksettan kan omfatta flera sektioner och kontrollpunkter.
5. Centralbron kan täcka endast en del av spåret utan extra grafnod.
6. Samma spår kan samtidigt visa Riksettan och Centralbron.
7. Namnlösa spår kan ingå i resvägar utan att tekniska ID:n visas för spelaren.
8. **Endast stationer** visas som destinationer. Stationen är ett enkelt stoppställe på rätt räls, vid spårslut eller genomgående spår; **inga plattformar krävs**. Ankomst bekräftas vid rätt stoppunkt även om vagnen rullar. **Framme** visas tills spelaren lämnar minecarten; då avslutas navigationen.
9. En omöjlig sväng i en korsning används aldrig av ruttberäkningen.
10. Automatisk vänster/höger/rakt-instruktion använder verklig in- och utgående geometri.
11. En anpassad instruktion som `Välj knappen för Martyrön` har företräde.
12. Administratören kan se zoner, spår, riktningar, övergångar, namn och fel visuellt.
13. Vanlig navigation fungerar utan JourneyMap.
14. JourneyMap kan visa samma nät och aktiva resväg när integrationen är installerad.
15. En avbruten inspelning lämnar ingen delvis aktiv data.
16. En felaktig ändring kan inte ersätta den senaste fungerande revisionen.
17. En namngivningsbehörig redaktör kan markera två punkter, välja rätt kandidatväg och namnge de befintliga spåren utan att åka sträckan.
18. Geometriskt föreslagna spårpar förblir oparade tills en administratör uttryckligen godkänner parningen.
19. Separata serverbehörigheter styr inspektion, namngivning, inspelning och administration/publicering utan att ge onödig topologiåtkomst.
20. Vid **osäker position** visas destinationen men ingen sväng-/knappinstruktion förrän spåret identifierats.
21. En behörig spelare kan **starta survey var som helst på rälsen**, och publicering lyckas endast vid säker nätanslutning och full validering.
22. HUD visar högst **en huvudled och ett lokalt rälssträckenamn**; administratörens anpassade instruktioner visas **ordagrant**, oberoende av språk.
23. **Framme** visas kvar tills spelaren **stiger ur minecarten**, då navigeringen avslutas automatiskt.

## 20. Återstående detaljbeslut

Följande **tekniska detaljer**, som inte ändrar de fem fastställda produktbesluten, behöver prototypas eller beslutas under implementationen:

1. Exakt tolerans och algoritm för **geometriskt grundade** spårparningsförslag (alltid manuell bekräftelse).
2. Exakt format för geometri och detaljnivåer.
3. Hur portar redigeras när flera spår passerar zonkanten mycket nära varandra.
4. Hur en stabil och deterministisk prioritet väljs **inom** kategorierna huvudled respektive lokalt namn (beslutet om högst en av varje är redan fastställt).
5. Slutlig kommandosyntax och hur mycket administration som flyttas till grafiskt gränssnitt.
6. Exakta Fabric- och JourneyMap-API-versioner för målversionen.
7. Om ruttberäkningen primärt görs på servern, klienten eller hybridmässigt.
8. Hur tillfälliga avstängningar och enkelriktning under drift exponeras för administratörer.
9. Vilken databasimplementation och backupstrategi som bäst passar målservern.
10. Hur kandidater mellan två markerade punkter rankas när flera likvärdiga vägar finns.
11. Hur de **redan beslutade** separata serverbehörigheterna representeras som permission-noder och tilldelas användare.

## 21. Sammanfattning

Railway GPS ska modellera verklig räls, inte bara namngivna linjer mellan punkter. Kontrollpunkter representeras av tydliga 3D-zoner. Stationer är enkla stoppställen på rälsen, utan krav på plattformar. Korsningar har portar och verifierade övergångar. En spårsektion kan innehålla ett, två eller flera fysiska spår med separata riktningar och geometrier.

Namn är fristående lager. Riksettan kan löpa genom flera rödljus, Centralbron kan omfatta endast den stockmarkerade delen av två parallella spår och namnlösa anslutningar kan fortfarande användas fullt ut. En resa beräknas genom det gemensamma fysiska nätet och duplicerar inte spår för varje destinationskombination.

Klienten ska göra nätet synligt: zoner, spår, riktningar, övergångar, namn, aktiv resväg och valideringsfel ska kunna inspekteras i världen, styrt av separat inspektionsbehörighet. JourneyMap använder samma modell men förblir valfritt. **En fullständigt validerad survey publiceras direkt vid `finish`; inga obligatoriska granskningsutkast krävs.** Oparade parallella spår kan publiceras som separata spår, men spårparning kräver alltid administratörsbekräftelse. Spelarnas destinationer är enbart stationer. Ankomst kräver rätt räls/stoppzon men inte stillastående vagn; **Framme visas tills spelaren kliver ur minecarten**, varpå navigeringen avslutas automatiskt. Osäkert spår döljer alla sväng-/knappinstruktioner, survey får starta var som helst på rälsen med säker nätanslutning, och HUD visar högst en huvudled plus ett lokalt namn.

Moddet förblir vägledande och icke-ingripande. Det berättar vad spelaren ska göra men styr aldrig minecart, knappar, redstone eller växlar.
