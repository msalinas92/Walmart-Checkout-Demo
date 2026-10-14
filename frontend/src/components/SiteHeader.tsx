"use client";

import Link from "next/link";
import { useState } from "react";
import { useCart } from "../hooks/useCart";

type SiteHeaderProps = {
  search?: string;
  onSearch?: (value: string) => void;
};

export function SiteHeader({ search, onSearch }: SiteHeaderProps) {
  const [menuOpen, setMenuOpen] = useState(false);
  const { units } = useCart();

  return <>
    <header className="topbar">
      <button className="menu" aria-label="Abrir menú" aria-expanded={menuOpen} onClick={() => setMenuOpen(true)}>☰</button>
      <Link className="brand" href="/" aria-label="Ir al catálogo">Walmart<span aria-hidden="true">✳</span></Link>
      {onSearch
        ? <label className="search"><input value={search} onChange={(event) => onSearch(event.target.value)} placeholder="Buscar TV, café, hogar…" aria-label="Buscar productos" /><b aria-hidden="true">⌕</b></label>
        : <div className="header-context">Compra online segura</div>}
      <Link className="bag" href="/carrito" aria-label={`Abrir carrito con ${units} productos`}>🛒 <span>{units}</span></Link>
    </header>

    <div className={`drawer-backdrop ${menuOpen ? "open" : ""}`} onClick={() => setMenuOpen(false)} />
    <aside className={`drawer ${menuOpen ? "open" : ""}`} aria-hidden={!menuOpen}>
      <div className="drawer-heading"><strong>Walmart<span>✳</span></strong><button onClick={() => setMenuOpen(false)} aria-label="Cerrar menú">×</button></div>
      <p>Explora la demo</p>
      <Link href="/" onClick={() => setMenuOpen(false)}>⌂ Inicio y catálogo <span>›</span></Link>
      <Link href="/carrito" onClick={() => setMenuOpen(false)}>🛒 Mi carrito <span>{units}</span></Link>
      <Link href="/checkout" onClick={() => setMenuOpen(false)}>✓ Finalizar compra <span>›</span></Link>
      <hr />
      <small>Catálogo y promociones administrados en Firestore. Órdenes proyectadas desde Pub/Sub hacia BigQuery.</small>
    </aside>
  </>;
}
