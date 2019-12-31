package main

import (
	"database/sql"
	"fmt"
	_ "github.com/mattn/go-sqlite3"
	"log"
	"net/http"
	"encoding/csv"
	"os"
	"io"
)

func fetchCsv(db *sql.DB ) {
	resp, _ := http.Get("http://standards-oui.ieee.org/oui/oui.csv")
	defer resp.Body.Close()
	r := csv.NewReader(resp.Body)
	r.Read()
	for {
		record, err := r.Read()
		if err == io.EOF {
			break
		}
		mac := ""
		for i, char := range record[1] {
			if i > 0 && i % 2 == 0 {
				mac += ":"
			}
			mac += string(char)
		}
		write(db, mac, record[2])
		println(record[2])
		fmt.Println(mac)
	}

}

func write(db *sql.DB, mac string, text string) {
	tx, err := db.Begin()
	if err != nil {
		log.Fatal(err)
	}
	stmt, err := tx.Prepare("insert into macvendor (name, mac) values(?, ?)")
	if err != nil {
		log.Fatal(err)
	}
	defer stmt.Close()
	_, err = stmt.Exec(text, mac)
	if err != nil {
		log.Fatal(err)
	}
	tx.Commit()
}

func main() {
	dbFile := "./app/src/main/assets/mac_devices.db"
	os.Remove(dbFile)

	db, err := sql.Open("sqlite3", dbFile)

	if err != nil {
		log.Fatal(err)
	}
	defer db.Close()

	sqlStmt := `
	create table macvendor (name TEXT not null, mac TEXT not null, PRIMARY KEY (name, mac));
	`
	_, err = db.Exec(sqlStmt)
	if err != nil {
		log.Printf("%q: %s\n", err, sqlStmt)
		return
	}
	fetchCsv(db)

}