"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useCart } from "../hooks/useCart";
import { categoryIcons, money, requestQuote, type Quote } from "../lib/checkout";
import { CheckoutSteps } from "./CheckoutSteps";
import { SiteHeader } from "./SiteHeader";

export function CartPage() {
  const { cart, hydrated, units, grossTotal, changeQuantity } = useCart();
  const [quote, setQuote] = useState<Quote | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [quoting, setQuoting] = useState(false);

  useEffect(() => {
    if (!hydrated || cart.length === 0) { setQuote(null); return; }
    const controller = new AbortController();
    setQuoting(true);
    // Aquí cotizo con transferencia para mostrar promociones de producto y carrito antes de elegir el pago.
    requestQuote(cart, "TRANSFER", controller.signal)
      .then((result) => { setQuote(result); setError(null); })
      .catch((reason: Error) => { if (reason.name !== "AbortError") setError(reason.message); })
      .finally(() => { if (!controller.signal.aborted) setQuoting(false); });
    return () => controller.abort();
  }, [cart, hydrated]);

  const totalDiscount = quote?.discounts.reduce((total, discount) => total + discount.amount, 0) ?? 0;

  return <main>
    <SiteHeader />
    <section className="shell page-shell">
      <CheckoutSteps current={2} />
      <div className="page-heading"><div><p className="eyebrow">PASO 2 DE 3</p><h1>Revisa tu carrito</h1><p>Ajusta cantidades y revisa las promociones antes de elegir cómo pagar.</p></div><Link href="/">← Seguir comprando</Link></div>

      {!hydrated
        ? <div className="card loading-card">Cargando tu selección…</div>
        : cart.length === 0
          ? <section className="card empty empty-page"><span>🛒</span><h2>Tu carrito está vacío</h2><p>Explora el catálogo y agrega productos para comenzar.</p><Link className="primary-link" href="/">Ir al catálogo</Link></section>
          : <div className="checkout-grid">
              <section className="products card">
                <div className="section-title"><h2>Productos <span>({units})</span></h2><span className="edit">Guardado en este navegador</span></div>
                {cart.map((item) => <article className="product" key={item.id}>
                  <div className="product-art"><span>{categoryIcons[item.category] ?? "□"}</span></div>
                  <div className="product-info"><p>{item.category}</p><h3>{item.name}</h3>{item.productDiscountPercent > 0 && <small>Oferta de producto −{item.productDiscountPercent}%</small>}<button className="remove" onClick={() => changeQuantity(item.id, 0)}>Eliminar</button></div>
                  <div className="quantity"><button onClick={() => changeQuantity(item.id, item.quantity - 1)} aria-label={`Disminuir ${item.name}`}>−</button><b>{item.quantity}</b><button onClick={() => changeQuantity(item.id, item.quantity + 1)} aria-label={`Aumentar ${item.name}`}>+</button></div>
                  <div className="product-price"><strong>{money.format(item.unitPrice * item.quantity)}</strong><small>precio de lista</small></div>
                </article>)}
              </section>

              <aside className="summary card">
                <div className="summary-heading"><h2>Resumen del carrito</h2>{quoting && <span>Actualizando…</span>}</div>
                <div className="line"><span>Productos</span><span>{money.format(grossTotal)}</span></div>
                {quote?.discounts.map((discount) => <div className="line discount" key={discount.code}><span>{discount.name}</span><span>−{money.format(discount.amount)}</span></div>)}
                <hr />
                <div className="line"><span>Envío estándar</span><span className="free">Sin costo</span></div>
                <div className="total"><span>Total antes del pago</span><strong>{money.format(quote?.total ?? grossTotal)}</strong></div>
                {totalDiscount > 0 && <p className="saving">Ya ahorras {money.format(totalDiscount)}</p>}
                <p className="payment-hint">En el siguiente paso puedes obtener un descuento adicional según el medio de pago.</p>
                <Link className="primary-link" href="/checkout">Continuar al pago <span>→</span></Link>
                <small className="safe">🔒 Pago ficticio · Sin datos bancarios</small>
              </aside>
            </div>}
      {error && <p className="notice error">{error}</p>}
    </section>
  </main>;
}
