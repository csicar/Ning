package main

import (
	"database/sql"
	"fmt"
	_ "github.com/mattn/go-sqlite3"
	"log"
	"net/http"
	"encoding/csv"
	"strconv"
	"os"
	"io"
)

func fetchCsv(db *sql.DB ) {
	resp, _ := http.Get("https://www.iana.org/assignments/service-names-port-numbers/service-names-port-numbers.csv")
	defer resp.Body.Close()
	r := csv.NewReader(resp.Body)
	r.Read()
	for {
		record, err := r.Read()
		if err == io.EOF {
			break
		}
		serviceName := record[0]
		if len(serviceName) > 0 {

			portNumber, _ := strconv.ParseInt(record[1], 10, 32)
			transportProtocol := record[2]
			serviceDescription := record[3]

			write(db, int(portNumber), serviceName, serviceDescription, transportProtocol)
			fmt.Printf("service: %s port: %s, tr: %s, descr: %s", serviceName, portNumber, transportProtocol, serviceDescription)
			fmt.Println(record)
		}
	}

}

func write(db *sql.DB, port int, service string, description string, transportProtocol string) {
	tx, err := db.Begin()
	if err != nil {
		log.Fatal(err)
	}
	stmt, err := tx.Prepare("insert into portmap (name, port, description, transport) values(?, ?, ?, ?)")
	if err != nil {
		log.Fatal(err)
	}
	defer stmt.Close()
	_, err = stmt.Exec(service, port, description, transportProtocol)
	if err != nil {
		log.Fatal(err)
	}
	tx.Commit()
}

func main() {
	dbFile := "./app/src/main/assets/port_map.db"
	os.Remove(dbFile)

	db, err := sql.Open("sqlite3", dbFile)

	if err != nil {
		log.Fatal(err)
	}
	defer db.Close()

	sqlStmt := `
	create table portmap (portmapId INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT not null, port INT not null, description Text not null, transport Text not null);
	`
	_, err = db.Exec(sqlStmt)
	if err != nil {
		log.Printf("%q: %s\n", err, sqlStmt)
		return
	}
	fetchCsv(db)

}