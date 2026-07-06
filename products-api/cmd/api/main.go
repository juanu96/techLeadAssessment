package main

import (
	"context"
	"errors"
	"fmt"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"strings"
	"syscall"
	"time"

	"github.com/juanu96/techLeadAssessment/products-api/internal/httpapi"
	"github.com/juanu96/techLeadAssessment/products-api/internal/product"
)

func main() {
	logger := slog.New(slog.NewJSONHandler(os.Stdout, nil))
	if err := run(logger); err != nil {
		logger.Error("products API stopped", "error", err)
		os.Exit(1)
	}
}

func run(logger *slog.Logger) error {
	port := strings.TrimSpace(os.Getenv("PRODUCTS_API_PORT"))
	if port == "" {
		return errors.New("PRODUCTS_API_PORT is required")
	}

	server := &http.Server{
		Addr:              ":" + port,
		Handler:           httpapi.NewHandler(product.NewCatalog()),
		ReadHeaderTimeout: 5 * time.Second,
	}

	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	serverErrors := make(chan error, 1)
	go func() {
		logger.Info("products API listening", "port", port)
		serverErrors <- server.ListenAndServe()
	}()

	select {
	case err := <-serverErrors:
		if errors.Is(err, http.ErrServerClosed) {
			return nil
		}
		return fmt.Errorf("listen: %w", err)
	case <-ctx.Done():
		shutdownCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
		defer cancel()
		if err := server.Shutdown(shutdownCtx); err != nil {
			return fmt.Errorf("shutdown: %w", err)
		}
		return nil
	}
}
