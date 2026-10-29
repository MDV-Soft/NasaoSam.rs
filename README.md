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