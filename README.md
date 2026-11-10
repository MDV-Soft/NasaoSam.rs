# NasaoSam – backend

Backend: Spring Boot 4, Java 21, MySQL 8.4, Flyway, JWT.

## Preduslovi

- **JDK 21**
- **MySQL 8.4** na portu **3307** (namerno ne 3306, da se ne sudara sa drugim lokalnim serverima)
- IntelliJ IDEA (preporučeno)

Maven nije potrebno instalirati, projekat koristi Maven wrapper (`mvnw`).

## Lokalna baza

Kao `root` korisnik na MySQL 8.4 (port 3307) pokreni:

```sql
CREATE DATABASE nasaosam CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'nasaosam'@'localhost' IDENTIFIED BY 'nasaosam';
GRANT ALL PRIVILEGES ON nasaosam.* TO 'nasaosam'@'localhost';
```

Korisnik `nasaosam` ima pristup samo bazi `nasaosam`. Tabele pravi Flyway automatski pri pokretanju.

## Pokretanje

1. Otvori projekat u IntelliJ-u i sačekaj da Maven preuzme zavisnosti.
2. Proveri da je JDK projekta 21 (File → Project Structure).
3. Pokreni `NasaoSamApplication`.
4. Provera: `curl.exe http://localhost:8080/actuator/health` treba da vrati `{"status":"UP"}`.

Tajne (`HMAC_SECRET`, `JWT_SECRET` i ostale) se čitaju iz promenljivih okruženja.
Vrednosti u `application.yml` važe **samo za lokalni razvoj**.

## Pravila rada u timu

- **Niko ne radi direktno na `main`.** Svaka funkcija ide na svoju granu: `feature/naziv`.
- Spajanje u `main` isključivo kroz **Pull Request**, uz pregled drugog člana.
- Pre početka rada: `git checkout main` pa `git pull`.
- **Flyway migracije:** jednom izvršena migracija se nikad ne menja, svaka izmena ide u novi fajl.
  Pre pravljenja nove migracije dogovorite se ko uzima koji broj (`V2`, `V3`...), da ne nastanu dve sa istim brojem.
- Zaustavi aplikaciju pre pravljenja novog fajla migracije (DevTools inače izvrši prazan fajl).

## Dogovor sa frontendom: heš imena sa dokumenta

Pravo ime nikad ne napušta telefon. Frontend šalje SHA-256 heš, a backend preko njega primenjuje HMAC.
Heš je isti samo ako je ulaz isti slovo za slovo, zato se ime pre heširanja svodi na isti oblik:

1. Ime i prezime su **odvojena polja**.
2. Ćirilica u latinicu (Љ → lj, Њ → nj, Џ → dž...).
3. Uklanjanje kvačica: č, ć → c; š → s; ž → z; đ → dj.
4. Mala slova, bez razmaka na početku i kraju, više razmaka zaredom u jedan.
5. Datum rođenja u obliku `GGGG-MM-DD`.
6. Konačan tekst:
    - `nameHash`: `prezime|ime`
    - `nameDobHash`: `prezime|ime|GGGG-MM-DD`
7. SHA-256 nad UTF-8 tekstom, kao heksadecimalni string malim slovima (64 znaka).

Primer: „Петровић Ђорђе”, rođen 14.05.1999 → `petrovic|djordje|1999-05-14`.

## API

| Metoda | Putanja | Pristup |
|---|---|---|
| GET | `/actuator/health` | javno |
| POST | `/api/auth/register` | javno |
| POST | `/api/auth/login` | javno |
| GET | `/api/auth/me` | prijavljen korisnik |

Svaka izmena API-ja se upisuje u ovu tabelu, da frontend zna šta je promenjeno.


KORACI _________________________________________________ IZMENE ___________________________________ NAPREDAK :

Faza 1 (Osnove i postavka ): ----------------------------------------- GOTOVO ------------------------
	1. Postavka MySQL, Git.... ////////////
	2. Migracija V1 tabela users, entiteti i repo za users ////////////
	3. Registracija, Prijava i JWT ////////////
	4. Handler za greske i ApiError ////////////
	5. CORS ////////////
	6. Enumi za vrste predmeta, vrste dokumanata i boje predmeta ////////////
	7. HmacServis za hesovanje imena ////////////
	8. AES sifrovanje i konverter za osetljiva polja ////////////
	9. Sva vremena u UTC ////////////
Faza 2 (Izgubljeni oglasi): ------------------------------------------ GOTOVO ---------------------------
	10. Migracija V2 lost_report, lost_report_items ////////////
	11. Entiteti, repo za izgubljene oglase ////////////
	12. Servisi i kontroleri za izgubljene oglase ////////////
	13. Ocena kompletnosti oglasa ////////////
	14. API endpoint za izgubljene oglase ////////////
Faza 3 (Zajednicka Infrastruktura): ------------------- GOTOVO SINE ----------------------------
	15. enumi za FE	//////////////
	16. Telefon pri registraciji coevka koji je izgubio oglas migracaija V3 //////////////
	17. OSTATAK (mejlovi, potvrda, itd itd) NIJE POTREBNO ZA MVP ----------- POSLE
Faza 4 (Nalazac):
	18. Migracija V4 found_reports, found_report_items
	19. entiteti, statusi repo za nadjen predmet
	20. Otpremanje slike (do 5mb)
	21. Uklanjanje EXIF podataka
	22. cuvanje na Cloudinary
	23. ruta za AI predlog predmeta
	24. ONE TIME TOKEN DA NE MORA SE KREIRA NALOG za nalazaca i LINK ZA NALAZACA	
	25. servis za prijavu nalaza
	26. Zastita nalazaca i one time tokena
Faza 5 (AI---pomozi boze):
	27.podesavnje AI provajdera itd
	28. Analiza slike, predlog predmeta
	29. Popunjavanje forme vlasnika iz slobodnog teksta
	30. AI sudija
	31. AI provera odgovora na kontrolono pitanje
	32. kontrola troskova AI tokena
Faza 6 (spajanje):
	33. Migracija V5 matches, par oglas-nalaz
	34. Dogadjaji posle upisa, aktiviran nalaz, nov oglas, izmenjem oglas
	35. upit za kandidate
	36. bodovanje parova predmeta
	37. AI sudija za najblizih 10 predmeta
	38. ponovno spajanje ako je izmenjen oglas
	39. TESTIRANJE AI ZA SVE
Faza 7 (provera vlasnika) :
	40. Migracija v6 za verifikaciju
	41. vlasnik treba da vidi poklapanja
	42. odgovor na kontrolno pitanje "ovo je moje"
	43. nalazac vidi odgovor i potvrdjuje ili odbija
	44. placanje vraceno itd (NISMO SIGURNI JOS)
Faza 8 (bezbednost i privatnost):
	45. verifikacija mejla,
	46. zakazani poslovi istek oglasa, nalaza, brisanje itd itd
	47. zadrzavanje osetljivih podataka (treba se brisu)
	48. RATE LIMITING sine
Faza 9 (deploy):
	49. dokumentacija, provera, CORS
	50. BOZE POMOZI AKO ZNAS



