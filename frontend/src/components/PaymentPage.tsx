"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { useCart } from "../hooks/useCart";
import { apiBaseUrl, money, paymentCopy, requestQuote, type Order, type PaymentMethod, type Quote } from "../lib/checkout";
import { CheckoutSteps } from "./CheckoutSteps";
import { SiteHeader } from "./SiteHeader";

export function PaymentPage() {
  const { cart, hydrated, units, grossTotal, clear } = useCart();
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>("DEBIT");
  const [quote, setQuote] = useState<Quote | null>(null);
  const [order, setOrder] = useState<Order | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [quoting, setQuoting] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const idempotencyKey = useRef<string | null>(null);

  useEffect(() => {
    if (!hydrated || cart.length === 0) { setQuote(null); return; }
    const controller = new AbortController();
    setQuoting(true);
    requestQuote(cart, paymentMethod, controller.signal)
      .then((result) => { setQuote(result); setError(null); })
      .catch((reason: Error) => { if (reason.name !== "AbortError") setError(reason.message); })
      .finally(() => { if (!controller.signal.aborted) setQuoting(false); });
    return () => controller.abort();
  }, [cart, hydrated, paymentMethod]);

  async function confirmCheckout() {
    if (!quote) return;
    setConfirming(true);
    idempotencyKey.current ??= crypto.randomUUID();
    try {
      // Aquí reutilizo la misma clave si el usuario reintenta, para no duplicar la compra ni el evento.
      const response = await fetch(`${apiBaseUrl}/checkouts/confirm`, {
        method: "POST",
        headers: { "Content-Type": "application/json", "Idempotency-Key": idempotencyKey.current },
        body: JSON.stringify({ quoteId: quote.quoteId })
      });
      if (!response.ok) throw new Error((await response.json() as { message?: string }).message ?? "No pude confirmar la compra.");
      const confirmedOrder = await response.json() as Order;
      setOrder(confirmedOrder);
      setError(null);
      clear();
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Ocurrió un error inesperado.");
    } finally {
      setConfirming(false);
    }
  }

  const totalDiscount = quote?.discounts.reduce((total, discount) => total + discount.amount, 0) ?? 0;

  if (order) return <main>
    <SiteHeader />
    <section className="shell confirmation-page">
      <CheckoutSteps current={3} />
      <section className="confirmation-card card">
        <span className="confirmation-icon">✓</span>
        <p className="eyebrow">COMPRA CONFIRMADA</p>
        <h1>¡Todo listo!</h1>
        <p>Simulamos el pago y registramos la transacción para el flujo analítico.</p>
        <div className="confirmation-number"><span>Número de orden</span><code>{order.orderId}</code></div>
        <div className="confirmation-total"><span>Total pagado con {paymentCopy[order.quote.paymentMethod].title}</span><strong>{money.format(order.quote.total)}</strong></div>
        <div className="event-proof"><b>Pub/Sub</b><span>Evento publicado</span><i>→</i><b>BigQuery</b><span>Proyección analítica</span></div>
        <Link className="primary-link" href="/">Volver al catálogo</Link>
      </section>
    </section>
  </main>;

  return <main>
    <SiteHeader />
    <section className="shell page-shell">
      <CheckoutSteps current={3} />
      <div className="page-heading"><div><p className="eyebrow">PASO 3 DE 3</p><h1>Elige cómo pagar</h1><p>El total cambia en tiempo real según el beneficio de cada medio de pago.</p></div><Link href="/carrito">← Volver al carrito</Link></div>

      {!hydrated
        ? <div className="card loading-card">Preparando el pago…</div>
        : cart.length === 0
          ? <section className="card empty empty-page"><span>🛒</span><h2>No hay productos para pagar</h2><p>Agrega productos o revisa nuevamente tu carrito.</p><Link className="primary-link" href="/">Ir al catálogo</Link></section>
          : <div className="payment-layout">
              <section className="payment-column">
                <section className="payment card">
                  <div><p className="eyebrow">MEDIO DE PAGO</p><h2>Selecciona una opción</h2></div>
                  <div className="payment-options">{(Object.keys(paymentCopy) as PaymentMethod[]).map((method) => <button key={method} className={`payment-option ${paymentMethod === method ? "selected" : ""}`} onClick={() => setPaymentMethod(method)}><span className="radio" /><span><b>{paymentCopy[method].title}</b><small>{paymentCopy[method].description}</small></span>{paymentCopy[method].badge && <em>{paymentCopy[method].badge}</em>}</button>)}</div>
                </section>
                <section className="card order-preview"><div className="section-title"><h2>Tu pedido <span>({units})</span></h2><Link href="/carrito">Editar</Link></div>{cart.map((item) => <div className="preview-line" key={item.id}><span>{item.quantity}×</span><p>{item.name}</p><strong>{money.format(item.unitPrice * item.quantity)}</strong></div>)}</section>
              </section>

              <aside className="summary card">
                <div className="summary-heading"><h2>Total final</h2>{quoting && <span>Calculando…</span>}</div>
                <div className="line"><span>Productos</span><span>{money.format(grossTotal)}</span></div>
                {quote?.discounts.map((discount) => <div className="line discount" key={discount.code}><span>{discount.name}</span><span>−{money.format(discount.amount)}</span></div>)}
                <hr />
                <div className="line"><span>Envío estándar</span><span className="free">Sin costo</span></div>
                <div className="total"><span>Total a pagar</span><strong>{money.format(quote?.total ?? grossTotal)}</strong></div>
                {totalDiscount > 0 && <p className="saving">Ahorro total {money.format(totalDiscount)}</p>}
                <button className="primary" disabled={!quote || quoting || confirming} onClick={() => void confirmCheckout()}>{confirming ? "Confirmando compra…" : `Pagar ${money.format(quote?.total ?? grossTotal)}`}</button>
                <small className="safe">🔒 Pago simulado · La orden usa una clave idempotente</small>
              </aside>
            </div>}
      {error && <p className="notice error">{error}</p>}
    </section>
  </main>;
}
