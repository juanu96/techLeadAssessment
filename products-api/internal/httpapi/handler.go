package httpapi

import (
	"encoding/json"
	"net/http"
	"strings"

	"github.com/juanu96/techLeadAssessment/products-api/internal/product"
)

type Handler struct {
	catalog *product.Catalog
}

func NewHandler(catalog *product.Catalog) http.Handler {
	handler := &Handler{catalog: catalog}
	mux := http.NewServeMux()
	mux.HandleFunc("/health", handler.health)
	mux.HandleFunc("/products/", handler.productByID)
	return mux
}

func (h *Handler) health(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		methodNotAllowed(w)
		return
	}

	writeJSON(w, http.StatusOK, map[string]string{"status": "UP"})
}

func (h *Handler) productByID(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		methodNotAllowed(w)
		return
	}

	productID := strings.TrimPrefix(r.URL.Path, "/products/")
	if productID == "" || strings.Contains(productID, "/") {
		writeJSON(w, http.StatusNotFound, map[string]string{"error": "product not found"})
		return
	}

	item, found := h.catalog.FindByID(productID)
	if !found {
		writeJSON(w, http.StatusNotFound, map[string]string{"error": "product not found"})
		return
	}

	writeJSON(w, http.StatusOK, item)
}

func methodNotAllowed(w http.ResponseWriter) {
	w.Header().Set("Allow", http.MethodGet)
	writeJSON(w, http.StatusMethodNotAllowed, map[string]string{"error": "method not allowed"})
}

func writeJSON(w http.ResponseWriter, status int, value any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(value)
}
