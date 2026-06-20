package recursos;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class ListaDoble implements Iterable<Object> {

    protected NodoDoble cabeza;
    protected NodoDoble cola;

    protected int tamanio;

    public ListaDoble() {
        cabeza = null;
        cola = null;
        tamanio = 0;
    }

    public boolean isEmpty() {
        return tamanio == 0;
    }

    public void add(Object info) { //agrega al final//
        NodoDoble nuevo = new NodoDoble(info);

        if (isEmpty()) {
            cabeza = nuevo;
            cola = nuevo;

        } else {
            nuevo.setPrevNodo(cola);

            cola.setNextNodo(nuevo);
            cola = nuevo;
        }
        tamanio++;
    }

    public void addFirst(Object info) {
        NodoDoble nuevo = new NodoDoble(info);

        if (isEmpty()) {
            cabeza = nuevo;
            cola = nuevo;

        } else {
            nuevo.setNextNodo(cabeza);

            cabeza.setPrevNodo(nuevo);
            cabeza = nuevo;
        }
        tamanio++;
    }

    public void insert(int indice, Object info) {

        if (indice < 0 || indice > tamanio) {
            throw new IndexOutOfBoundsException();
        }
        if (indice == 0) {
            addFirst(info);
            return;
        }
        if (indice == tamanio) {
            add(info);
            return;
        }
        NodoDoble actual = getNodo(indice);

        NodoDoble anterior = actual.getPrevNodo();

        NodoDoble nuevo = new NodoDoble(info, anterior, actual);

        anterior.setNextNodo(nuevo);
        actual.setPrevNodo(nuevo);
        tamanio++;
    }

    public boolean remove(Object info) {
        NodoDoble actual = cabeza;

        while (actual != null) {
            if (actual.getNodoInfo().equals(info)) {
                desconectar(actual);
                tamanio--;
                return true;
            }
            actual = actual.getNextNodo();
        }
        return false;
    }

    public void clear() {
        cabeza = null;
        cola = null;
        tamanio = 0;
    }

    private void desconectar(NodoDoble nodo) {
        NodoDoble prev = nodo.getPrevNodo();
        NodoDoble next = nodo.getNextNodo();

        if (prev != null) {
            prev.setNextNodo(next);
        } else {
            cabeza = next;
        }
        if (next != null) {
            next.setPrevNodo(prev);
        } else {
            cola = prev;
        }
    }

    @Override
    public Iterator<Object> iterator() {

        return new Iterator<Object>() {

            private NodoDoble actual = cabeza;

            @Override
            public boolean hasNext() {
                return actual != null;
            }

            @Override
            public Object next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                Object dato = actual.getNodoInfo();
                actual = actual.getNextNodo();
                return dato;
            }
        };
    }
    public NodoDoble getCabeza() {
        return cabeza;
    }

    public NodoDoble getCola() {
        return cola;
    }

    public NodoDoble buscarNodo(Object info) {
        NodoDoble actual = cabeza;
        while (actual != null) {
            if (actual.getNodoInfo().equals(info)) {
                return actual;
            }
            actual = actual.getNextNodo();
        }
        return null;
    }
}
