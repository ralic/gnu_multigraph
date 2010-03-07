/* This file is part of 'MultiGraph'
 *
 * Copyright (C) 2009 Paul Jakma
 *
 * MultiGraph is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 3, or (at your option) any
 * later version.  
 * 
 * MultiGraph is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.   
 *
 * You should have received a copy of the GNU General Public License
 * along with MultiGraph.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.nongnu.multigraph;

import java.util.*;

/**
 * A multi-edge, directed graph implementation of the {@link Graph} interface.
 * <p>
 * This implementation allows for multiple, directed edges between any nodes,
 * including between the same node.
 * <p>
 * XXX: As SimpleDiGraph inherits from this, this class probably should not be
 * public. We don't really want different restrictions of the graph to be 
 * type-compatible.
 */
public class MultiDiGraph<N,L>
       extends Observable
       implements Graph<N,L> {

  // Hash of user-specific N-type node objects to internal Node objects
  HashMap<N,Node<N,L>> nodes;
  private Set<N> nodeset;
  
  public MultiDiGraph () {
    nodes = new HashMap<N,Node<N,L>> ();
    nodeset = nodes.keySet();
  }
  
  /* Get the internal Node for the given user_node, creating as needs be */
  final Node<N,L> get_node (N user_node) {
    Node<N,L> n = nodes.get (user_node);
    
    if (n == null) {
      n = new Node<N,L> (user_node);
      nodes.put (user_node, n);
    }
    
    return n;
  }
  
  /* This is something I'd stick in a macro in C, but dont really have them with
   * java..
   */
  final void _set (Node<N,L> nf, Node<N,L> nt, int weight, L label) {
    assert nf != null;
    assert nt != null;
    assert label != null;
    
    nf.set (nt, weight == 0 ? weight : 1, label);
  }
  
  /* Exported to package so that simpler graphs can be subclassed from this */
  protected void _set (N from, N to, int weight, L label) {
    Node<N,L> nf, nt = null;
    
    assert (from != null);
    
    nf = get_node (from);
    
    if (to == null || to == from)
      nt = nf;
    else 
      nt = get_node (to);
    
    _set (nf, nt, weight, label);
    
    setChanged ();
    notifyObservers (from);
    notifyObservers (to);
  }
  
  // Set an edge between the two nodes (the nodes are added as required)
  public synchronized void set (N from, N to, L label)
    { _set (from, to, 1, label); }
  // Same, but for a weighted edge
  public synchronized void set (N from, N to, L label, int weight)
    { _set (from, to, weight, label); }
  
  protected boolean _remove (N from, N to, L label) {
    Node<N,L> nf, nt = null;
    
    if (from == null)
      throw new NullPointerException ("remove: 'from' must not be null");
    if (to == null)
      throw new NullPointerException ("remove: 'from' must not be null");
    
    if ((nf = nodes.get (from)) == null)
      return false;
    
    nt = ((to == from) ? nf : nodes.get (to));
    
    if (nt == null)
      return false;
    
    setChanged ();
    notifyObservers (from);
    
    // Dispatch to appropriate method
    if (label != null)
      return nf.remove (nt, label);
    
    notifyObservers (to);
    
    return nf.remove (nt);
  }
  
  public synchronized boolean remove (N from, N to, L label) {
    return _remove (from, to, label);
  }
  
  public synchronized boolean remove (N from, N to) {
    return _remove (from, to, null);
  }
  
  public synchronized Set<Edge<N,L>> edges (N from) {
    Node<N,L> n;
    Set<Edge<N,L>> edges;
    
    n = nodes.get (from);

    if (n == null)
      return null;

    return Collections.unmodifiableSet (n.edges ());
  }
  
  public synchronized Collection<Edge<N,L>> edges (N from, N to) {
    Node<N,L> nf, nt;
    Collection<Edge<N,L>> edges;
    
    if ((nf = nodes.get (from)) == null)
      return null;
    if ((nt = nodes.get ((to))) == null)
      return null;
    
    return Collections.unmodifiableCollection (nf.edges (nt));
  }
  
  public Edge<N, L> edge (N from, N to) {
    Collection<Edge<N,L>> edges = edges (from, to);

    if (edges == null)
      return null;

    for (Edge<N,L> e : edges)
      return e;
    
    return null;
  }

  public Edge<N, L> edge (N from, N to, L label) {
    Collection<Edge<N,L>> edges = edges (from, to);

    if (edges == null)
      return null;
    
    for (Edge<N,L> e : edges)
      if (e.label () == label)
        return e;

    return null;
  }

  public synchronized Set<N> successors (N node) {
    Set<N> sc = new HashSet<N> ();
    Node<N,L> n;
    Set<Edge<N,L>> edges;

    assert node != null;
    
    if ((n = nodes.get (node)) == null)
      return null;
    
    for (Edge<N,L> e : n.edges ())
      sc.add (e.to());
    
    return sc;
  }

  public synchronized int edge_outdegree (N node) {
    return get_node(node).edge_outdegree ();
  }
  
  public synchronized int nodal_outdegree (N node) {
    return get_node(node).nodal_outdegree();
  }
  
  public synchronized float avg_nodal_degree () {
    float avg = 0;
    int num = 0;
    
    for (Node<N,L> n : nodes.values ()) {
      num++;
      avg += (n.nodal_outdegree () - avg) / num;
    }
    return avg;
  }
  
  public synchronized int max_nodal_degree () {
    int max = 0;
    int d;
    
    for (Node<N,L> n : nodes.values ()) {
      if ((d = n.nodal_outdegree ()) > max)
        max = d;
    }
    
    return max;
  }
  
  public synchronized String toString () {
    StringBuilder sb = new StringBuilder ();
    for (Node<N,L> n : nodes.values ()) {
      sb.append (n + "\n");
      for (Edge<N,L> e : n.edges ())
        sb.append ("\t" + e + "\n");        
    }
    return sb.toString ();
  }
  
  /* Collection/Set interfaces
   *
   * Ideally, we'd have just extended an existing Set class, however that
   * isn't really possible as we also require some kind of Map view to the
   * /same/ data (we can get a Set view of a Map, but not the other way around
   * it seems; maintaining two seperate collections for the same data is
   * wasteful and error-prone).
   *
   * We can't extend a map implementation, as the Map interface isn't a
   * subset of the desired Graph interface.
   *
   * The approach used therefore is to manually map the Set interface to the
   * methods of a Set view of the private, nodes HashMap. (Is there a better
   * way?)
   *
   * In an ideal world, we'd implement some kind of 'MapSet' hybrid, go
   * through the process of getting it incorporated into some future release
   * of JavaSE Collections framework and then extend that. That might take
   * too long ;).
   */
  
  // Add operations add disconnected nodes.
  public boolean add (N node) { return (get_node (node) != null); }
  public boolean addAll (Collection<? extends N> c) {
    return nodeset.addAll (c);
  }
  public void clear () { 
    nodes.clear ();
    setChanged ();
    notifyObservers ();
  }
  public boolean contains (Object o) { return nodeset.contains (o); }
  public boolean containsAll (Collection<?> c) { 
    return nodeset.containsAll (c);
  }
  public boolean equals (Object o) { return nodeset.equals (o); }
  public int hashCode () { return nodeset.hashCode (); }
  public boolean isEmpty () { return nodeset.isEmpty (); }
  public int size () { return nodeset.size (); }
  
  public Object[] toArray () { return nodeset.toArray (); }
  @SuppressWarnings("hiding")
  public <N> N[] toArray (N[] a) { return nodeset.toArray (a); }
  public Iterator<N> iterator() { return nodeset.iterator (); }
  
  public void clear_all_edges () {
    for (Node<N,L> n : nodes.values ())
      n.clear ();
    
    setChanged ();
    notifyObservers ();
  }

  /* same reasoning as above for the unchecked */
  @SuppressWarnings ("unchecked")
  public synchronized boolean remove (Object o) {
    Node<N,L> node = nodes.get ((N) o);
    boolean ret = false;

    if (node == null)
      return false;

    for (Object oe : node.edges ().toArray ()) {
      Edge<N,L> e = (Edge<N,L>) oe;
      if (remove (e.from (), e.to (), e.label ()))
        ret = true;
    }
    
    notifyObservers (o);
    
    return nodeset.remove (o) ? true : ret;
  }
  public boolean removeAll (Collection<?> c) {
    int size1 = size ();
    
    for (Object o : c)
      remove (o);
    
    return size1 == size ();
  }
  public boolean retainAll (Collection<?> c) {
    int size1 = size ();
    
    for (N node : this)
      if (!c.contains (node))
        remove (node);
    
    return size1 == size ();
  }
  
  /* Extension to Observable to coalesce updates somewhat */
  private boolean plugObservable = false;
  private boolean notifyAll = false;
  private Set<Object> notifyObjs = new HashSet<Object> ();
  
  public void plugObservable () {
    plugObservable = true;
    notifyAll = false;
  }
  public void unplugObservable () {
    if (!plugObservable)
      return;
    
    plugObservable = false;
    
    if (notifyAll)
      super.notifyObservers ();
    else
      for (Object o : notifyObjs)
        super.notifyObservers (o);
    notifyObjs.clear ();
  }
  
  @Override
  public void notifyObservers () {
    if (!plugObservable) {
      super.notifyObservers (null);
      return;
    }
    notifyAll = true;
  }

  @Override
  public void notifyObservers (Object arg) {
    if (!plugObservable) {
      if (arg == null)
        notifyAll = true;
      super.notifyObservers (arg);
      return;
    }
    if (!notifyAll)
      notifyObjs.add (arg);
  }
  
}
