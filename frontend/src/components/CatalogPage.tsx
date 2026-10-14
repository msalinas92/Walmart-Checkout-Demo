"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useCart } from "../hooks/useCart";
import { apiBaseUrl, categoryIcons, money, type Product } from "../lib/checkout";
import { CheckoutSteps } from "./CheckoutSteps";
import { SiteHeader } from "./SiteHeader";

export function CatalogPage() {
  const [catalog, setCatalog] = useState<Product[]>([]);
  const [categories, setCategories] = useState<string[]>([]);
  const [search, setSearch] = useState("");
  const [category, setCategory] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const { cart, units, add } = useCart();

  useEffect(() => {
    fetch(`${apiBaseUrl}/catalog/categories`)
      .then(async (response) => {
        if (!response.ok) throw new Error("No pude cargar las categorías.");
        return response.json() as Promise<string[]>;
      })
      .then(setCategories)
      .catch((reason: Error) => setError(reason.message));
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    const timer = window.setTimeout(() => {
      setLoading(true);
      // Aquí cancelo la consulta anterior para que el catálogo siempre refleje el último filtro escrito.
      fetch(`${apiBaseUrl}/catalog/products?query=${encodeURIComponent(search)}&category=${encodeURIComponent(category)}`, { signal: controller.signal })
        .then(async (response) => {
          if (!response.ok) throw new Error("No pude cargar el catálogo desde Firestore.");
          return response.json() as Promise<Product[]>;
        })
        .then((products) => { setCatalog(products); setError(null); })
        .catch((reason: Error) => { if (reason.name !== "AbortError") setError(reason.message); })
        .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    }, 250);
    return () => { window.clearTimeout(timer); controller.abort(); };
  }, [search, category]);

  return <main>
    <SiteHeader search={search} onSearch={setSearch} />
    <section className="shell page-shell">
      <CheckoutSteps current={1} />
      <section className="store-hero">
        <div><p className="eyebrow">COMPRA INTELIGENTE</p><h1>Encuentra todo para tu día</h1><p>Productos reales desde Firestore y promociones calculadas por la API en cada paso.</p></div>
        <div className="hero-proof"><b>Envío gratis</b><span>en esta demostración</span></div>
      </section>

      <section className="catalog card" id="catalogo">
        <div className="catalog-heading"><div><p className="eyebrow">CATÁLOGO</p><h2>Productos destacados</h2></div><span>{loading ? "Buscando…" : `${catalog.length} resultados`}</span></div>
        <div className="categories" aria-label="Categorías">
          {["", ...categories].map((item) => <button key={item || "all"} className={category === item ? "selected" : ""} onClick={() => setCategory(item)}>{item || "Todos"}</button>)}
        </div>
        {error && <p className="notice error">{error}</p>}
        {!loading && catalog.length === 0
          ? <div className="empty"><span>⌕</span><h3>No encontramos productos</h3><p>Prueba otra búsqueda o categoría.</p><button onClick={() => { setSearch(""); setCategory(""); }}>Limpiar filtros</button></div>
          : <div className="product-grid">{catalog.map((product) => {
              const offerPrice = product.unitPrice * (1 - product.productDiscountPercent / 100);
              const inCart = cart.find((item) => item.id === product.id)?.quantity ?? 0;
              return <article className="catalog-product" key={product.id}>
                {product.productDiscountPercent > 0 && <span className="offer-badge">−{product.productDiscountPercent}%</span>}
                <div className="catalog-art" aria-hidden="true">{categoryIcons[product.category] ?? "□"}</div>
                <p>{product.category}</p><h3>{product.name}</h3>
                <div className="catalog-price"><strong>{money.format(offerPrice)}</strong>{product.productDiscountPercent > 0 && <s>{money.format(product.unitPrice)}</s>}</div>
                <button className="primary" onClick={() => add(product)}>{inCart > 0 ? `Agregar otra · ${inCart} en carrito` : "Agregar al carrito"}</button>
              </article>;
            })}</div>}
      </section>
    </section>

    {units > 0 && <div className="cart-dock"><div><span>🛒</span><p><b>{units} {units === 1 ? "producto" : "productos"}</b><small>Tu selección se guardó</small></p></div><Link href="/carrito">Ver carrito <span>→</span></Link></div>}
  </main>;
}
