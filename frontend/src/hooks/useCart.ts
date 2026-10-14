"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import type { CartItem, Product } from "../lib/checkout";

const cartStorageKey = "walmart-demo-cart-v1";
const cartEventName = "walmart-demo-cart-change";

function readCart(): CartItem[] {
  try {
    const stored = window.localStorage.getItem(cartStorageKey);
    return stored ? JSON.parse(stored) as CartItem[] : [];
  } catch {
    return [];
  }
}

export function useCart() {
  const [cart, setCart] = useState<CartItem[]>([]);
  const [hydrated, setHydrated] = useState(false);

  useEffect(() => {
    setCart(readCart());
    setHydrated(true);
    const synchronize = (event: Event) => setCart((event as CustomEvent<CartItem[]>).detail ?? readCart());
    window.addEventListener(cartEventName, synchronize);
    window.addEventListener("storage", synchronize);
    return () => {
      window.removeEventListener(cartEventName, synchronize);
      window.removeEventListener("storage", synchronize);
    };
  }, []);

  const persist = useCallback((update: (current: CartItem[]) => CartItem[]) => {
    setCart((current) => {
      const next = update(current);
      window.localStorage.setItem(cartStorageKey, JSON.stringify(next));
      queueMicrotask(() => window.dispatchEvent(new CustomEvent(cartEventName, { detail: next })));
      return next;
    });
  }, []);

  const add = useCallback((product: Product) => persist((current) => {
    const existing = current.find((item) => item.id === product.id);
    if (!existing) return [...current, { ...product, quantity: 1 }];
    return current.map((item) => item.id === product.id ? { ...item, quantity: item.quantity + 1 } : item);
  }), [persist]);

  const changeQuantity = useCallback((productId: string, quantity: number) => persist((current) =>
    current.flatMap((item) => item.id !== productId ? [item] : quantity > 0 ? [{ ...item, quantity }] : [])), [persist]);

  const clear = useCallback(() => persist(() => []), [persist]);
  const units = useMemo(() => cart.reduce((total, item) => total + item.quantity, 0), [cart]);
  const grossTotal = useMemo(() => cart.reduce((total, item) => total + item.unitPrice * item.quantity, 0), [cart]);

  return { cart, hydrated, units, grossTotal, add, changeQuantity, clear };
}
